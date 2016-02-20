// 
//  Chain.java
// 
//  ***************************************************************************
// 
//  Copyright 2008, Stanford University
// 
//  Permission to use, copy, modify, and distribute this software and its
//  documentation for any purpose and without fee is hereby granted,
//  provided that the above copyright notice appear in all copies and that
//  both that copyright notice and this permission notice appear in
//  supporting documentation, and that the name of Stanford University not
//  be used in advertising or publicity pertaining to distribution of the
//  software without specific, written prior permission.  Stanford
//  University makes no representations about the suitability of this
//  software for any purpose.  It is provided "as is" without express or
//  implied warranty.
// 
//  ***************************************************************************
// 
//  $Id$
// 

package edu.stanford.math.plex;

/**
 * A <code>Chain</code> instance is an element of the module constructed by
 * taking formal sums of ring elements times simplices. In our case the
 * ring will always be a field, so the module is a vector space where the
 * basis elements are indexed by simplices. The dimension of the space of
 * chains is huge, so instead of making vectors of length the dimension of
 * the space, we use a sparse representation with (sorted) Simplex[] arrays
 * and corresponding int[] coefficient arrays. Addition and finding the
 * coefficient of the largest (non-zero) basis element are the most
 * important in persistence calculations, and this representation is suited
 * to these operations.
 *
 * <p> See Persistence, and references therein for the real story.
 *
 * @see        edu.stanford.math.plex.Persistence
 *
 * @version $Id$
 */
public final class Chain {
   
  /**
   * Characteristic of the field of coefficients.
   * <p>
   * This will be a prime. Someday we might allow 0, for the rationals.
   * <p>
   */
  final int p;

  /**
   * The implementation we use here is that the chain is given by an
   * sorted array of Simplex instances, and a paired array (of the same
   * length) of coefficients of those instances.
   */
  final int[] coefficients;
  final Simplex[] simplices;

  /**
   * Return the largest basis element.
   * <p>
   *
   * @return     Largest simplex with nonzero coefficient.
   */
  public final Simplex maxS() {
    return simplices[simplices.length-1];
  }

  /**
   * Return the coefficient of the largetst basis element.
   * <p>
   *
   * @return     Coefficient of largest simplex with nonzero coefficient.
   */
  public final int maxC() {
    return coefficients[coefficients.length-1];
  }

  // Chain constructor is called only from within this file.
  private Chain (int modulus, int[] cvec, Simplex[] elts) {
    assert(s_and_c_okay (elts, cvec, modulus));
    p = modulus;
    coefficients = cvec;
    simplices = elts;
  }

  // Do a modified merge/sort on the basis elements of the chains that we
  // are combining.
  private final Chain mergeBasisElts(Simplex[] first, int[] fc, 
                                     Simplex[] second, int[] sc, int c) {
    int counter = 0;
    int fi = 0;
    int si = 0;

    // we have to make an initial pass through the two Chains to figure
    // out the length of the composite Chain.
    while ((fi < first.length) || (si < second.length)) {

      while ((fi < first.length) && 
             ((si == second.length) || first[fi].slt(second[si]))) {
        counter++;
        fi++;
      }

      if ((fi < first.length) && (si < second.length) && first[fi].seq(second[si])) {
        if (((fc[fi] + (c * sc[si])) % p) != 0) 
          counter++;
        fi++;
        si++;
      }

      while ((si < second.length) && 
             ((fi == first.length) || second[si].slt(first[fi]))) {
        counter++;
        si++;
      }
    }

    // Everything cancels, so we return null.
    if (counter == 0)
      return null;

    int[] newC = new int[counter];
    Simplex[] newS = new Simplex[counter];
    int index = 0;
    fi = 0;
    si = 0;
    while ((fi < first.length) || (si < second.length)) {

      while ((fi < first.length) && 
             ((si == second.length) || first[fi].slt(second[si]))) {
        newS[index] = first[fi];
        newC[index] = fc[fi];
        index++;
        fi++;
      }

      if ((fi < first.length) && (si < second.length) && first[fi].seq(second[si])) {
        if (((fc[fi] + (c * sc[si])) % p) != 0) {
          newS[index] = first[fi];
          newC[index] = ((fc[fi] + (c * sc[si])) % p);
          index++;
        }
        fi++;
        si++;
      }

      while ((si < second.length) && 
             ((fi == first.length) || second[si].slt(first[fi]))) {
        newS[index] = second[si];
        newC[index] = (c * sc[si]) % p;
        index++;
        si++;
      }
    }

    return new Chain(p, newC, newS);
  }


  /**
   * Add one chain to another.
   * <p>
   *
   * <p>
   *
   * @param      x  the chain to add
   * @param      c  a coefficient to apply to the argument chain before adding.
   * @return     the result of the addition
   */
  public final Chain add(Chain x, int c) {
    if (c == 0)
      return this;
    assert (this.p == x.p);
    return mergeBasisElts(simplices, coefficients,
                          x.simplices, x.coefficients, c);
  }

  // Normalize the elements of a chain.
  private final static Chain normalize(Simplex[] s, int[] c, int p) {
    int counter = 0;
    for (int i = 0; i < s.length; i++) {
      if ((c[i] != 0) && (s[i] != null))
        counter++;
    }

    // if everything cancels, we return null.
    if (counter == 0)
      return null;

    int[] newC = new int[counter];
    Simplex[] newS = new Simplex[counter];
    int index = 0;

    for (int i = 0; i < s.length; i++) {
      if ((c[i] != 0) && (s[i] != null)) {
        newS[index] = s[i];
        newC[index] = c[i];
        index++;      
      }
    }

    chain_sort(newS, newC);

    return new Chain(p, newC, newS);
  }

  // Sort the elements of b, and in the course of doing so, do an
  // identical rearrangment of the elements of c. NOTE: Doing a
  // bubblesort on a long array would be inefficient, but this function
  // is only used to convert a "Simplex boundary" into a chain, and so
  // the length of b will always be so short that bubblesort would be the
  // first choice, anyway.
  private static final void chain_sort (Simplex[] b, int[] c) {
    for (int j = b.length - 1; j > 0; j--) {
      for (int i = 0; i < j; i++) {
        if (b[i+1].slt(b[i])) {
          Simplex dummyS = b[i];
          int dummyC = c[i];
          b[i] = b[i+1];
          b[i+1] = dummyS;
          c[i] = c[i+1];
          c[i+1] = dummyC;
        }
      }
    }
  }

  // Make "boundary coefficients".
  private static int[] alternatingCoefficients(int mod, int len) {
    int[] return_value = new int[len];
    int sign = 1;
    for (int i = 0; i < len; i++) {
      if (sign > 0)
        return_value[i] = 1;
      else
        return_value[i] = mod - 1;
      sign = -sign;
    }
    return return_value;
  }

  /**
   * Convert a boundary array to a chain. The non-null Simplex entries
   * must have their filtration indices set already.  <p>
   *
   * @param      boundary  the boundary array to convert
   * @return     the chain that corresponds to the boundary
   */
  static Chain fromBoundary(Simplex[] boundary, int p) {
    if (boundary == null)
      return null;
    int[] c = alternatingCoefficients(p, boundary.length);
    return normalize(boundary, c, p);
  }

  /**
   * Represent a Chain as a String.
   *
   * <p>
   * @return     A string that describes a Chain.
   */
  public String toString() { 
    if (simplices.length == 0)
      return "0";
    String return_value = "";
    for (int i = 0; i < simplices.length-1; i++) {
      return_value = return_value + String.format("%d*", coefficients[i]) + 
        simplices[i].toString() + " + ";
    }
    return_value = return_value + String.format("%d*", coefficients[simplices.length-1]) + 
      simplices[simplices.length-1].toString();
    return return_value;
  }


  //
  // Test code 
  // 
  // The code below this point is for testing purposed only.
  //


  /**
   * Compute the boundary of a chain.
   * <p>
   *
   *
   * @return     the boundary chain
   */
  Chain boundary(int p) {
    int first_index = 0;
    Chain first = null;
    while((first == null) && (first_index < simplices.length)){
      first = fromBoundary((simplices[first_index]).boundary(), p);
      int first_c = coefficients[first_index];
      if (first != null) {
        for (int i = 0; i < first.simplices.length; i++)
          first.coefficients[i] = (first.coefficients[i] * first_c) % p;
      }
      first_index++;
    }
    if (first == null)
      return null;
    for (int i = first_index; i < simplices.length; i++) {
      Chain next = fromBoundary((simplices[i]).boundary(), p);
      if (next != null) {
        int next_c = coefficients[i];
        if (first != null)
          first = first.add(next, next_c);
        else {
          first = next;
          for (int j = 0; j < first.simplices.length; j++)
            first.coefficients[j] = (first.coefficients[j] * next_c) % p;
        }
      }
    }
    return first;
  }


  /**
   * Chain EQuals. Test function. 
   *
   * <p>
   * @param      c   Chain to compare.
   * @return     true is equal, else false.
   *
   */
  boolean ceq(Chain c) {
    if (simplices.length != c.simplices.length)
      return false;
    for (int i = 0; i < simplices.length; i++) {
      if ((coefficients[i] != c.coefficients[i]) ||
          !(simplices[i].seq(c.simplices[i])))
        return false;
    }
    return true;
  }

  /**
   * Return a random Chain. A test function.
   *
   * <p>
   * @return     A "random" Chain.
   */
  static Chain random(int p) { 
    int length = 1 + (int) Math.floor((Math.random() * 20.0));
    Simplex[] simplices = new Simplex[length];
    int[] coefficients = new int[length];
    for(int i = 0; i < length; i++) {
      simplices[i] = Simplex.random();
      coefficients[i] = (1 + (int) Math.floor((Math.random() * p))) % p;
      if (coefficients[i] == 0)
        coefficients[i] = 1;
    }
    assert(simplices != null);
    assert(coefficients != null);
    chain_sort (simplices, coefficients);
    {
      boolean loser = false;
      for(int i = 0; i < length-1; i++) {
        if (!(simplices[i].slt(simplices[i+1]))) {
          coefficients[i+1] = 0;
          loser = true;
        }
      }
      if (loser)
        return normalize(simplices, coefficients, p);
    }

    return new Chain(p, coefficients, simplices);
  }

  // Test function.
  static private boolean s_and_c_okay (Simplex[] s, int[] c, int p) {
    if ((s.length != c.length) || (s.length < 1)) return false;
    for (int i = 0; i < s.length; i++) {
      if ((c[i] < 1) || (c[i] > (p - 1)))
        return false;
    }
    for (int i = 0; i < s.length-1; i++) {
      if (!(s[i].slt(s[i+1])))
        return false;
    }
    return true;
  }
}
