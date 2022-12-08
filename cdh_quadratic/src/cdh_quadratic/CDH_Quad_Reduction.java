package cdh_quadratic;

import java.math.BigInteger;
import cdh.CDH_Challenge;
import cdh.I_CDH_Challenger;
import genericGroups.IGroupElement;

/**
 * This is the file you need to implement.
 * 
 * Implement the methods {@code run} and {@code getChallenge} of this class.
 * Do not change the constructor of this class.
 */
public class CDH_Quad_Reduction extends A_CDH_Quad_Reduction<IGroupElement> {

    /**
     * Do NOT change or remove this constructor. When your reduction can not provide
     * a working standard constructor, the TestRunner will not be able to test your
     * code and you will get zero points.
     */
    public CDH_Quad_Reduction() {
        // Do not add any code here!
    }

    @Override
    public IGroupElement run(I_CDH_Challenger<IGroupElement> challenger) {
        // This is one of the both methods you need to implement.

        // By the following call you will receive a DLog challenge.
        CDH_Challenge<IGroupElement> challenge = challenger.getChallenge();

        IGroupElement g = challenge.generator;
        IGroupElement gX = challenge.x;
        IGroupElement gY = challenge.y;

        IGroupElement gA = f4(g, g, g);
        IGroupElement gAXY = f4(g, gX, gY);

        // Below 2 lines have O(logp) complexity
        //IGroupElement cur = expo_gA(g, gA, gA.getGroupOrder().subtract(BigInteger.valueOf(3)));
        //IGroupElement gXY = f4(g, cur, gAXY);

        // Below 2 lines has constant calls to f4
        IGroupElement gA2 = f4(g, gA, g);
        IGroupElement gXY = f4(gA2, gAXY, g);

        // your reduction does not need to be tight. I.e., you may call
        // adversary.run(this) multiple times.

        // Remember that this is a group of prime order p.
        // In particular, we have a^(p-1) = 1 mod p for each a != 0.

        return gXY;
    }

    private IGroupElement generator = null;
    private IGroupElement gX = null;
    private IGroupElement gY = null;

    @Override
    public CDH_Challenge<IGroupElement> getChallenge() {

        // This is the second method you need to implement.
        // You need to create a CDH challenge here which will be given to your CDH
        // adversary.
        IGroupElement generator = this.generator;
        IGroupElement x = this.gX;
        IGroupElement y = this.gY;
        // Instead of null, your cdh challenge should consist of meaningful group
        // elements.
        CDH_Challenge<IGroupElement> cdh_challenge = new CDH_Challenge<IGroupElement>(generator, x, y);

        return cdh_challenge;
    }

    private IGroupElement f1(IGroupElement g, IGroupElement gX, IGroupElement gY) {
        this.generator = g;
        this.gX = gX;
        this.gY = gY;
        IGroupElement result = adversary.run(this);
        return result;
    }

    private IGroupElement f2(IGroupElement g, IGroupElement gX, IGroupElement gY) {
        BigInteger b0 = BigInteger.valueOf(0);
        IGroupElement gD = f1(g, gX.power(b0), gY.power(b0));
        IGroupElement res = f1(g, gX, gY);
        return res.multiply(gD.invert());
    }

    private IGroupElement f3(IGroupElement g, IGroupElement gX, IGroupElement gY) {
        BigInteger b0 = BigInteger.valueOf(0);
        IGroupElement gCY_D = f1(g, gX.power(b0), gY);
        IGroupElement res = f1(g, gX, gY);
        return res.multiply(gCY_D.invert());
    }

    private IGroupElement f4(IGroupElement g, IGroupElement gX, IGroupElement gY) {
        BigInteger b0 = BigInteger.valueOf(0);
        IGroupElement gBX_D = f1(g, gX, gY.power(b0));
        IGroupElement gAXY_BX = f3(g, gX, gY);
        IGroupElement gD = f1(g, gX.power(b0), gY.power(b0));
        return gAXY_BX.multiply(gBX_D.invert()).multiply(gD);
    }

    private IGroupElement expo_gA(IGroupElement g, IGroupElement gA, BigInteger b) {
        if (b.intValue() == 0)
            return gA.power(b);
        else if (b.intValue() == 1)
            return gA.clone();
        IGroupElement cur = gA.clone();
        IGroupElement gA2 = f4(g, gA, g);
        for (int i = b.bitLength() - 2; i >= 0; i--) {
            cur = f4(g, cur, cur);
            if (!b.testBit(i))
                cur = f4(gA2, cur, g);
        }
        return cur;
    }

}
