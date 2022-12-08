package katzwang.reductions;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ddh.DDH_Challenge;
import ddh.I_DDH_Challenger;
import genericGroups.IGroupElement;
import katzwang.A_KatzWang_EUFNMA_Adversary;
import katzwang.KatzWangPK;
import katzwang.KatzWangSignature;
import katzwang.KatzWangSolution;
import utils.Triple;

public class KatzWang_EUFNMA_Reduction extends A_KatzWang_EUFNMA_Reduction {

    public KatzWang_EUFNMA_Reduction(A_KatzWang_EUFNMA_Adversary adversary) {
        super(adversary);
        // Do not change this constructor!
    }

    private IGroupElement g = null;
    private IGroupElement gX = null;
    private IGroupElement gY = null;
    private IGroupElement gZ = null;
    private Random ran = new Random();
    private Map<Triple<IGroupElement, IGroupElement, String>, BigInteger> map = new HashMap<>();

    @Override
    public Boolean run(I_DDH_Challenger<IGroupElement, BigInteger> challenger) {
        // Write your Code here!
        DDH_Challenge<IGroupElement> challenge = challenger.getChallenge();
        this.g = challenge.generator;
        this.gX = challenge.x;
        this.gY = challenge.y;
        this.gZ = challenge.z;

        KatzWangSolution<BigInteger> sol = this.adversary.run(this);
        if (sol == null)
            return false;
        String m = sol.message;
        KatzWangSignature<BigInteger> sig = sol.signature;
        BigInteger c = sig.c;
        BigInteger s = sig.s;

        BigInteger cNew = hash(this.g.power(s).multiply(this.gY.power(c).invert()), this.gX.power(s).multiply(this.gZ.power(c).invert()), m);
        return cNew.equals(c);
    }

    @Override
    public KatzWangPK<IGroupElement> getChallenge() {
        // Write your Code here!
        return new KatzWangPK<IGroupElement>(this.g, this.gX, this.gY, this.gZ);
    }

    @Override
    public BigInteger hash(IGroupElement comm1, IGroupElement comm2, String message) {
        // Write your Code here!
        Triple<IGroupElement, IGroupElement, String> t = new Triple<IGroupElement, IGroupElement, String>(comm1, comm2, message);
        if (map.containsKey(t))
            return map.get(t);
        BigInteger rand;
        do {
            rand = new BigInteger(this.g.getGroupOrder().bitLength(), ran);
        } while (rand.compareTo(this.g.getGroupOrder()) != -1);
        map.put(t, rand);
        return rand;
    }

}
