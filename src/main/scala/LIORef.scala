/** Mutable references.  This definition, which mimics the one currently used in
  * the LIO source code, differs from an old presentation [*] which put a
  * separate label on the contents of the reference.
  *
  * [*] Stefan et al, Flexible Dynamic Information Flow Control in the Presence
  *     of Exceptions, JFP 2017
 */

package edu.cmu.spf.lio

case class LIORef[L, T](label: L, var value: T) {
//  def flatMap
}
