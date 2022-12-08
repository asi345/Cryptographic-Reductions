package schnorr.reductions;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import dlog.DLog_Challenge;
import dlog.I_DLog_Challenger;
import genericGroups.IGroupElement;
import schnorr.I_Schnorr_EUFNMA_Adversary;
import schnorr.SchnorrSignature;
import schnorr.SchnorrSolution;
import schnorr.Schnorr_PK;
import utils.Pair;

public class Schnorr_EUFNMA_Reduction extends A_Schnorr_EUFNMA_Reduction{

    public Schnorr_EUFNMA_Reduction(I_Schnorr_EUFNMA_Adversary<IGroupElement, BigInteger> adversary) {
        super(adversary);
        //Do not change this constructor!
    }

    private IGroupElement generator = null; // g
    private IGroupElement y = null; // g^x
    private int seed = 0;
    private Random ran = new Random();
    private Map<Pair<String, IGroupElement>, BigInteger> map = new HashMap<>();

    @Override
    public Schnorr_PK<IGroupElement> getChallenge() {
        //Write your Code here!
        return new Schnorr_PK<IGroupElement>(this.generator, this.y);
    }

    @Override
    public BigInteger hash(String message, IGroupElement r) {
        //Write your Code here!
        Pair<String, IGroupElement> p = new Pair<String,IGroupElement>(message, r);
        if (map.containsKey(p))
            return map.get(p);
        BigInteger rand;
        do {
            rand = new BigInteger(this.generator.getGroupOrder().bitLength(), ran);
        } while (rand.compareTo(this.generator.getGroupOrder()) != -1);
        map.put(p, rand);
        return rand;
    }

    @Override
    public BigInteger run(I_DLog_Challenger<IGroupElement> challenger) {
        //Write your Code here!

        DLog_Challenge<IGroupElement> challenge = challenger.getChallenge();
        this.generator = challenge.generator;
        this.y = challenge.x;

        this.seed = 22953285;
        this.adversary.reset(this.seed);

        SchnorrSolution<BigInteger> sol1 = this.adversary.run(this);
        if (sol1 == null)
            return null;
        String m1 = sol1.message;
        SchnorrSignature<BigInteger> sig1 = sol1.signature;
        BigInteger c1 = sig1.c;
        BigInteger s1 = sig1.s;

        if (!verify(m1, c1, s1))
            return null;

        this.adversary.reset(this.seed);
        this.map.clear();

        SchnorrSolution<BigInteger> sol2 = this.adversary.run(this);
        if (sol2 == null)
            return null;
        String m2 = sol2.message;
        SchnorrSignature<BigInteger> sig2 = sol2.signature;
        BigInteger c2 = sig2.c;
        BigInteger s2 = sig2.s;

        if (!verify(m2, c2, s2))
            return null;

        BigInteger x = s1.subtract(s2).mod(this.generator.getGroupOrder());
        BigInteger div = c1.subtract(c2).modInverse(this.generator.getGroupOrder());
        x = x.multiply(div).mod(this.generator.getGroupOrder());
        return x;
    }

    private boolean verify(String m, BigInteger c, BigInteger s) {
        IGroupElement gR = this.generator.power(s).multiply(this.y.power(c).invert());
        BigInteger cNew = hash(m, gR);
        return c.equals(cNew);
    }
    
}