package edu.cmu.spf.lio.demo

import scala.collection.JavaConversions._

import org.apache.log4j.{ Logger, Level }

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.{ SQLContext }

import org.apache.spark.ml.util.{DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.{SparkSession, DataFrame, Dataset}

//import org.apache.spark.ml.param._
import org.apache.spark.ml.param._

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.sql.types._

import org.apache.spark.ml.{Pipeline, PipelineStage, Transformer}
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}

import scala.reflect.ClassTag

object SparkUtil {
  Logger.getLogger("Remoting").setLevel(Level.ERROR)
  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)
  Logger.getLogger("spark").setLevel(Level.ERROR)
  Logger.getLogger("spark").setLevel(Level.ERROR)
  Logger.getLogger("databricks").setLevel(Level.ERROR)
  Logger.getRootLogger().setLevel(Level.ERROR)

  val sql = SparkSession
    .builder
    .master("local[*]")
    .config("spark.ui.showConsoleProgress", false)
    .config("spark.sql.streaming.checkpointLocation", ".")
    .config("spark.driver.memory", "8g")
    .config("spark.executor.memory", "8g")
    .appName("SparkUtil")
    .getOrCreate()

  val sc = sql.sparkContext

  import org.apache.spark.SparkConf
  import org.apache.spark.rdd.RDD
  import org.apache.spark.sql.SparkSession
  import org.apache.spark.storage.StorageLevel
  import org.apache.spark.streaming.{Seconds, StreamingContext, Time}

//  val ssc = new StreamingContext(sql.sparkContext, Seconds(1))

  val csvReader = sql.read
//    .format("com.databricks.spark.csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .option("mode", "FAILFAST")
    .option("nullValue", "?")

  val streamReader = sql.readStream
    .option("mode", "PERMISSIVE")
    .option("nullValue", "?")

  val reader = sql.read
    .option("mode", "PERMISSIVE")
    .option("nullValue", "?")

  def csvWrite(
    dataframe: DataFrame,
    file: String,
    delimiter: String = ","): Unit = {
    dataframe.coalesce(1)
      .write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("delimiter", delimiter)
      .save(file)
  }

  def rdd[T: ClassTag](its: Seq[T]): RDD[T] = sc.parallelize(its)

  def shutdown: Unit = sc.stop

  /* this doesn't work, there is no SetType, only ArrayType */
  class TokenizerSet(val sep: String = ",", override val uid: String)
    extends UnaryTransformer[String, Set[String], TokenizerSet] {

    override protected def outputDataType: DataType =
      new ArrayType(StringType, true)

    override protected def createTransformFunc: String => Set[String] = {
      originStr => sep.split(originStr).toSet
    }
  }

  /*
  class Renamer[IN](override val uid: String)
      extends UnaryTransformer[IN,IN,Renamer[IN]]
      with DefaultParamsWritable {

    def this() = this(Identifiable.randomUID("Renamer"))

//    override def setInputCol(value: String): T = set(inputCol, value).asInstanceOf[T]
//    override def setOutputCol(value: String): T = set(outputCol, value).asInstanceOf[T]

    var out_type: DataType = null

    override def transform(df: Dataset[_]): DataFrame = {
      out_type = df.schema(getInputCol).dataType
      df.withColumnRenamed(getInputCol, getOutputCol)
   }

    def createTransformFunc: IN => IN = {
      in => in
    }
    def outputDataType: DataType = out_type
  }*/

  class Renamer(
    val inputCol: String,
    val outputCol: String,
    val uid: String = Identifiable.randomUID("Renamer"))
      extends Transformer {

    def transformSchema(schema: StructType): StructType = {
      println(s"transformSchema: rename column $inputCol to $outputCol")
      val inputType = schema(inputCol).dataType
      val nullable  = schema(inputCol).nullable
        if (schema.fieldNames.contains(outputCol)) {
          throw new IllegalArgumentException(s"Output column ${outputCol} already exists.")
        }
        val outputFields = schema.fields :+
        StructField(outputCol, inputType, nullable = nullable)
        StructType(outputFields)
    }

    def transform(df: Dataset[_]): DataFrame = {
      println(s"transform: rename column $inputCol to $outputCol")
      df.withColumnRenamed(inputCol, outputCol)
   }

    def copy(extra: ParamMap): Renamer = {
      new Renamer(inputCol, outputCol, uid)
    }
  }

    class Dropper(
    val inputCol: String,
    val uid: String = Identifiable.randomUID("Dropper"))
      extends Transformer {

      def transformSchema(schema: StructType): StructType = {
        println(s"transformSchema: delete column $inputCol")
        if (! schema.fieldNames.contains(inputCol)) {
          throw new IllegalArgumentException(s"Input column ${inputCol} does not exists.")
        }
        StructType(schema.fields.filter{f => f.name != inputCol})
    }

      def transform(df: Dataset[_]): DataFrame = {
        println(s"transform: delete column $inputCol")
        df.drop(inputCol)
      }

    def copy(extra: ParamMap): Dropper = {
      new Dropper(inputCol, uid)
    }
  }

  def buildFeatures(
    _data: DataFrame,
    input_columns: Set[String],
    output_column: String="features",
    exclude_in_vector: Set[String] = Set[String]()
  ): DataFrame = {

    println("replacing missing values")
    val data = _data
      .na.fill("")
      .na.fill(0.0)
      .na.fill(0).persist

    val set_columns = input_columns.toSet

    val schema = data.schema

    val stages = data.columns.zip(data.schema.fields)
      .filter{case (col_name, _) => set_columns.contains(col_name)}
      .flatMap {
      case (col_name, col_info) =>
        //println(s"${col_name} ${col_info.dataType} ${col_info.metadata}")

          val new_name = "_" + col_name
          col_info.dataType match {
            case StringType => List(new StringIndexer()
                .setInputCol(col_name)
                .setOutputCol(new_name),
              new Dropper(col_name),
              new Renamer(new_name, col_name)
            )
            case DoubleType | IntegerType => List()
              //val temp1 = new Renamer(col_name, new_name)
              //List(temp)
          }
    }

    val pipeline = new Pipeline().setStages(stages)
    println("fitting transformers")
    val trans = pipeline.fit(data)
    println("running transformers")

    val tempData = trans.transform(data)

    val newSchema = StructType(tempData.schema.fields.map{ f =>
      StructField(f.name, f.dataType, f.nullable)
    })

    val tempData2 = sql.createDataFrame(tempData.rdd, newSchema)

    val vectorizer = new VectorAssembler()
      .setInputCols(input_columns.diff(exclude_in_vector).toArray)
      .setOutputCol(output_column)

    vectorizer.transform(tempData2)
  }
}

object StringUtil {
  /* https://gist.github.com/carymrobbins/7b8ed52cd6ea186dbdf8 */
/**
  *  Pretty prints a Scala value similar to its source represention.
  *  Particularly useful for case classes.
  *  @param a - The value to pretty print.
  *  @param indentSize - Number of spaces for each indent.
  *  @param maxElementWidth - Largest element size before wrapping.
  *  @param depth - Initial depth to pretty print indents.
  *  @return
  */
  def prettyPrint(a: Any, indentSize: Int = 2,
        maxElementWidth: Int = 30, depth: Int = 0): String = {
        val indent = " " * depth * indentSize
        val fieldIndent = indent + (" " * indentSize)
            val thisDepth = prettyPrint(_: Any, indentSize,
              maxElementWidth, depth)
            val nextDepth = prettyPrint(_: Any, indentSize,
              maxElementWidth, depth + 1)
        a match {
          // Make Strings look similar to their literal form.
          case s: String =>
            val replaceMap = Seq(
              "\n" -> "\\n",
              "\r" -> "\\r",
              "\t" -> "\\t",
              "\"" -> "\\\""
            )
                    '"' + replaceMap.foldLeft(s) { case (acc, (c, r))
                      => acc.replace(c, r) } + '"'
                  // For an empty Seq just use its normal String
                  //representation.
          case xs: Seq[_] if xs.isEmpty => xs.toString()
          case xs: Seq[_] =>
                    // If the Seq is not too long, pretty print on one
                    //line.
            val resultOneLine = xs.map(nextDepth).toString()
                    if (resultOneLine.length <= maxElementWidth)
                      return resultOneLine
                    // Otherwise, build it with newlines and proper
                    //field indents.
                    val result = xs.map(x =>
                      s"\n$fieldIndent${nextDepth(x)}").toString()
                    result.substring(0, result.length - 1) + "\n" +
            indent + ")"
            // Product should cover case classes.
          case p: Product =>
            val prefix = p.productPrefix
                    // We'll use reflection to get the constructor arg
                    //names and values.
            val cls = p.getClass
                    val fields =
                      cls.getDeclaredFields.filterNot(_.isSynthetic).map(_.getName)
            val values = p.productIterator.toSeq
                    // If we weren't able to match up fields/values,
                    //fall back to toString.
                    if (fields.length != values.length) return p.toString
            fields.zip(values).toList match {
                        // If there are no fields, just use the normal
                        //String representation.
              case Nil => p.toString
                          // If there is just one field, let's just
                          //print it as a wrapper.
              case (_, value) :: Nil =>
                s"$prefix(${thisDepth(value)})"
                          // If there is more than one field, build up
                          //the field names and values.
              case kvps =>
                            val prettyFields = kvps.map { case (k, v)
                              => s"$fieldIndent$k = ${nextDepth(v)}" }
                            // If the result is not too long, pretty
                            //print on one line.
                            val resultOneLine =
                              s"$prefix(${prettyFields.mkString(", ")})"
                            if (resultOneLine.length <=
                              maxElementWidth) return resultOneLine
                            // Otherwise, build it with newlines and
                            //proper field indents.
                s"$prefix(\n${prettyFields.mkString(",\n")}\n$indent)"
            }
                  // If we haven't specialized this type, just use its
                  //toString.
          case _ => a.toString
        }
      }
}

object SerialUtil {
  import java.io._
  import java.nio.file.{Paths, Files}

  /* https://gist.github.com/ramn/5566596 */
  class ObjectInputStreamWithCustomClassLoader(
    fileInputStream: FileInputStream
  ) extends ObjectInputStream(fileInputStream) {
    override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
      try { Class.forName(desc.getName, false, getClass.getClassLoader) }
      catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
    }
  }

  def loadOrMake[T](filename: String)(f: => T): T = {
    if (Files.exists(Paths.get(filename))) {
      println(s"$filename exists, loading")
      load(filename)
    } else {
      println(s"$filename does not exist, computing")
      val temp:T = f
      write(filename, temp)
      temp
    }
  }

  def load[T](filename: String): T = {
    val ois = new ObjectInputStreamWithCustomClassLoader(
      new FileInputStream(filename)
    )
    val e = ois.readObject.asInstanceOf[T]
    ois.close
    e
  }

  def write[T](filename: String, o: T): Unit = {
    val oos = new ObjectOutputStream(new FileOutputStream(filename))
    oos.writeObject(o)
    oos.close
  }


}
