package rsapkcs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import utils.Pair;

import static utils.NumberUtils.getRandomBigInteger;
import static utils.NumberUtils.ceilDivide;
import static utils.NumberUtils.getCeilLog;

public class RSAPKCS_OWCL_Adversary implements I_RSAPKCS_OWCL_Adversary {
    public RSAPKCS_OWCL_Adversary() {
        // Do not change this constructor!
    }

    /*
     * @see basics.IAdversary#run(basics.IChallenger)
     */
    @Override
    public BigInteger run(final I_RSAPKCS_OWCL_Challenger challenger) {

        // Initialization
        BigInteger c = challenger.getChallenge();
        BigInteger n = challenger.getPk().N;
        BigInteger e = challenger.getPk().exponent;
        int k = (n.bitLength() - 1) / 8 + 1;
        BigInteger B = BigInteger.valueOf(2).pow(8 * (k - 2));
        BigInteger B2 = B.multiply(BigInteger.TWO);
        BigInteger B3 = B.multiply(BigInteger.valueOf(3));
        BigInteger s0 = BigInteger.ONE;
        BigInteger s = BigInteger.ONE;
        BigInteger sPrev;
        BigInteger c0 = BigInteger.ONE;
        BigInteger ri = BigInteger.ONE;
        BigInteger r = BigInteger.ONE;
        ArrayList<Pair<BigInteger, BigInteger>> M = new ArrayList<>();
        Random rand = new Random();
        boolean isReady = false;
        boolean isFirst = true;

        // Blinding
        try {
            isReady = challenger.isPKCSConforming(c);
        } catch (Exception ex) {
            System.out.println("PKCS conforming control threw exception!");
        }
        if (isReady) {
            s0 = BigInteger.ONE;
            c0 = c.add(BigInteger.ZERO);
        } else {
            try {
                do {
                    s0 = getRandomBigInteger(rand, BigInteger.valueOf(Integer.MAX_VALUE));
                    c0 = s.pow(e.intValue()).multiply(c).mod(n);
                } while (!challenger.isPKCSConforming(c.multiply(s0.modPow(e, n)).mod(n)));
            } catch (Exception ex) {
                System.out.println("PKCS conforming control threw exception!");
            }
        }
        M.add(new Pair<BigInteger, BigInteger>(B2, B3.subtract(BigInteger.ONE)));
        s = s0.add(BigInteger.ZERO);

        while (!(M.size() == 1 && M.get(0).first.equals(M.get(0).second))) {
        //while (isFirst) {

            sPrev = s.add(BigInteger.ZERO);
            // Starting the search
            if (isFirst) {
                s = ceilDivide(n, B3);
                isFirst = false;
                try {
                    while (!challenger.isPKCSConforming(c0.multiply(s.modPow(e, n)).mod(n)))
                        s = s.add(BigInteger.ONE);
                } catch (Exception ex) {
                    System.out.println("PKCS conforming control threw exception!");
                }
            } else {
                if (M.size() > 1) {
                    s = sPrev.add(BigInteger.ONE);
                    try {
                        while (!challenger.isPKCSConforming(c0.multiply(s.modPow(e, n)).mod(n)))
                            s = s.add(BigInteger.ONE);
                    } catch (Exception ex) {
                        System.out.println("PKCS conforming control threw exception!");
                    }
                } else {
                    BigInteger a = M.get(0).first;
                    BigInteger b = M.get(0).second;
                    ri = ceilDivide(b.multiply(sPrev).subtract(B2), n).multiply(BigInteger.TWO);
                    BigInteger lower = ceilDivide(B2.add(ri.multiply(n)), b);
                    // Solve the flooring problem!!
                    BigInteger higher = B3.add(ri.multiply(n)).subtract(BigInteger.ONE).divide(a);
                    s = lower.add(BigInteger.ZERO);
                    //System.out.println(ri + " " + higher.subtract(lower));
                    try {
                        while (!challenger.isPKCSConforming(c0.multiply(s.modPow(e, n)).mod(n))) {
                            //System.out.println("still searching");
                            
                            s = s.add(BigInteger.ONE);
                            if (s.compareTo(higher) == 1) { // Care here
                                ri = ri.add(BigInteger.ONE);
                                lower = ceilDivide(B2.add(ri.multiply(n)), b);
                                higher = B3.add(ri.multiply(n)).divide(a);
                                s = lower.add(BigInteger.ZERO);
                                //System.out.println(ri + " " + higher.subtract(lower));
                            }

                        }
                    } catch (Exception ex) {
                        System.out.println("PKCS conforming control threw exception!");
                    }
                }
            }

            // Narrowing set of solutions
            ArrayList<Pair<BigInteger, BigInteger>> Mnew = new ArrayList<>();
            BigInteger a, b;
            for (int i = 0; i < M.size(); i++) {
                a = M.get(i).first;
                b = M.get(i).second;
                BigInteger lower = ceilDivide(a.multiply(s).subtract(B3).add(BigInteger.ONE), n);
                BigInteger higher = ceilDivide(b.multiply(s).subtract(B2), n);
                r = lower.add(BigInteger.ZERO);
                while (r.compareTo(higher) < 1) {
                    BigInteger start = a.add(BigInteger.ZERO);
                    BigInteger prob = ceilDivide(B2.add(r.multiply(n)), s);
                    start = start.max(prob);
                    /*if (a.compareTo(prob) == -1)
                        start = prob.add(BigInteger.ZERO);*/
                    BigInteger end = b.add(BigInteger.ONE);
                    prob = B3.subtract(BigInteger.ONE).add(r.multiply(n)).divide(s);
                    end = end.min(prob);
                    /*if (b.compareTo(prob) == 1)
                        end = prob.add(BigInteger.ONE);*/
                    
                    r = r.add(BigInteger.ONE);
                    if (start.compareTo(end) != 1)
                        Mnew.add(new Pair<BigInteger,BigInteger>(start, end));
                }
            }
            M = merge(Mnew);
            //M = new ArrayList<>(Mnew);
            /*
            System.out.println(M.size());
            for (int i = 0; i < M.size(); i++)
                System.out.println(M.get(i));*/
        }

        BigInteger a = M.get(0).first;
        BigInteger m = a.multiply(s0.modInverse(n)).mod(n);
        byte[] bs = m.toByteArray();
        int index = 0;
        for (int i = 1; i < bs.length; i++) {
            if (bs[i] == (byte) 0) {
                index = i + 1;
            }
        }
        if (index == bs.length) {
            return BigInteger.ZERO;
        }
        byte[] ms = new byte[bs.length - index];
        for (int i = 0; i < ms.length; i++) 
            ms[i] = bs[index + i];

        return new BigInteger(1, ms);
    }

    /*BigInteger pow(BigInteger base, BigInteger exponent, BigInteger n) {
        BigInteger result = BigInteger.ONE;
        while (exponent.signum() > 0) {
          if (exponent.testBit(0)) result = result.multiply(base).mod(n);
          base = base.multiply(base).mod(n);
          exponent = exponent.shiftRight(1);
        }
        return result;
      }*/

    public ArrayList<Pair<BigInteger, BigInteger>> merge(ArrayList<Pair<BigInteger, BigInteger>> intervals) {
        if (intervals != null && intervals.size() == 0) {
            return null;
        }
        Collections.sort((List<Pair<BigInteger, BigInteger>>) intervals, new Comparator<Pair<BigInteger, BigInteger>>() {
    
          public int compare(Pair<BigInteger, BigInteger> o1, Pair<BigInteger, BigInteger> o2) {
            if(o1.first.compareTo(o2.first) == 1) {
              return 1;
            } else if (o1.first.compareTo(o2.first) == -1) {
              return -1;
            }
            return 0;
          }
        });
        List<Pair<BigInteger, BigInteger>> list = new ArrayList<Pair<BigInteger, BigInteger>>();
        list.add(intervals.get(0));
        for(int i=1; i < intervals.size(); i++) {
        Pair<BigInteger, BigInteger> interval = list.get(list.size() - 1);
        Pair<BigInteger, BigInteger> curr = intervals.get(i);
        BigInteger f, s;
          if (curr.first.compareTo(interval.second) != 1) {
            if (interval.first.compareTo(curr.first) == 1)
                f = curr.first;
            else
                f = interval.first;
            if (interval.second.compareTo(curr.second) == -1)
                s = curr.second;
            else
                s = interval.second;
            list.remove(list.size() - 1);
            list.add(new Pair<BigInteger, BigInteger>(f ,s));
          } else {
            list.add(curr);
          }
        }
        return (ArrayList<Pair<BigInteger, BigInteger>>) list;
      }
}