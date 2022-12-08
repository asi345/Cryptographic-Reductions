package reductions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import algebra.SimplePolynomial;
import dhi.DHI_Challenge;
import dhi.I_DHI_Challenger;
import dy05.DY05_PK;
import dy05.I_Selective_DY05_Adversary;
import genericGroups.IGroupElement;

public class DHI_DY05_Reduction implements I_DHI_DY05_Reduction {
    // Do not remove this field!
    private final I_Selective_DY05_Adversary adversary;

    public DHI_DY05_Reduction(I_Selective_DY05_Adversary adversary) {
        // Do not change this constructor!
        this.adversary = adversary;
    }

    private IGroupElement g = null;
    private IGroupElement h = null;
    private IGroupElement hB = null;
    private DHI_Challenge chal = null;
    private BigInteger[][] pascal = null;
    private int x = 0;
    private SimplePolynomial f = null;

    @Override
    public IGroupElement run(I_DHI_Challenger challenger) {
        // Write Code here!

        DHI_Challenge challenge = challenger.getChallenge();
        this.g = challenge.get(0);
        BigInteger order = this.g.getGroupOrder();

        Random r = new Random();

        // Construct the initial polynomial f coefficients
        this.chal = challenge;

        // Lookup for binomial coefficients
        this.pascal = new BigInteger[this.chal.size()][this.chal.size()];
        genPascal(this.pascal.length);
        
        /*
        for (int i = 0; i <= this.chal.size(); i++) {
            System.out.println("row" + i);
            for (int j = 0; j <= this.chal.size(); j++)
                System.out.println(this.pascal[i][j]);
        }*/

        // c_0, ..., c_(q - 1)
        BigInteger[] ci = new BigInteger[this.chal.size() - 1];
        for (int i = 0; i < ci.length; i++) {
            do {
                ci[i] = new BigInteger(order.bitLength(), r);
            } while (ci[i].compareTo(order) != -1);
        }
        // Degree is q - 1
        this.f = new SimplePolynomial(order, ci);

        // Start the reduced challenge and get a sign
        IGroupElement sign = this.adversary.run(this);
        
        // Get gamma values
        SimplePolynomial divisor = new SimplePolynomial(order, this.x, 1);

        SimplePolynomial div = this.f.div(divisor);
        SimplePolynomial rem = this.f.subtract(div.multiply(divisor));

        // Calculate g^(1/A)
        IGroupElement mult = gF(div.multiply(BigInteger.valueOf(-1)));
        IGroupElement res = sign.multiply(mult).power(rem.get(0).modInverse(order));

        return res;
    }

    @Override
    public void receiveChallengePreimage(int _challenge_preimage) throws Exception {
        // Write Code here!
        /* 
        if (_challenge_preimage == 0) {
            throw new Exception("Preimage set to 0!");
        }*/
        this.x = _challenge_preimage;

        // Construct the public key
        this.h = gF(this.f);
        this.hB = this.g.power(BigInteger.ZERO);
        for (int j = 1; j <= this.f.degree + 1; j++) {
            this.hB = this.hB.multiply(gBJ(j).power(this.f.get(j - 1)));
        }
    }

    @Override
    public IGroupElement eval(int preimage) {
        // Write Code here!
        if (this.x == preimage) {
            System.out.println("Preimages can not be the same as x0!");
            return null;
        }
        // Construct fi
        SimplePolynomial fi = this.f.div(new SimplePolynomial(this.g.getGroupOrder(), preimage, 1));
        // g^(fi(B))
        return gF(fi);
    }

    @Override
    public DY05_PK getPK() {
        // Write Code here!
        return new DY05_PK(this.h, this.hB);
    }

    private void genPascal(int size) {
        for (int n = 0; n < size; n++) {
            for (int k = 0; k <= n; k++) {
                if (k == 0 || k == n)
                    this.pascal[n][k] = BigInteger.ONE;
                else
                    this.pascal[n][k] = this.pascal[n - 1][k - 1].add(this.pascal[n - 1][k]);
            }
        }
    }

    // Inputs : preimage x, index j
    // Output : g^(B^j) = g^((A - x)^j)
    private IGroupElement gBJ(int j) {
        IGroupElement res = this.g.power(BigInteger.ZERO);
        BigInteger x0 = BigInteger.valueOf(-this.x);
        for (int i = 0; i <= j; i++) {
            BigInteger pow = this.pascal[j][i].multiply(x0.pow(j - i));
            res = res.multiply(this.chal.get(i).power(pow));
        }
        return res;
    }

    // Input : polynomial f
    // Output : g^(f(B))
    private IGroupElement gF(SimplePolynomial f) {
        IGroupElement res = this.g.power(BigInteger.ZERO);
        for (int j = 0; j <= f.degree; j++) {
            res = res.multiply(gBJ(j).power(f.get(j)));
        }
        return res;
    }
}
