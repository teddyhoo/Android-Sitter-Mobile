package com.leashtime.sitterapp.Exif;

public class Rational {
    private final long mNumerator;
    private final long mDenominator;

    public Rational(long nominator, long denominator) {
        mNumerator = nominator;
        mDenominator = denominator;
    }

    public Rational(int rr, int rw) {
        mNumerator = rr;
        mDenominator = rw;
    }

    /**
     * Create a copy of a Rational.
     */
    public Rational(Rational r) {
        mNumerator = r.mNumerator;
        mDenominator = r.mDenominator;
    }
    /**
     * Gets the numerator of the rational.
     */
    public long getNumerator() {
        return mNumerator;
    }
    /**
     * Gets the denominator of the rational
     */
    public long getDenominator() {
        return mDenominator;
    }
    /**
     * Gets the rational value as type double. Will cause a divide-by-zero error
     * if the denominator is 0.
     */
    public double toDouble() {

        double retVal = (double)mNumerator / mDenominator;
        return retVal;
        // return mNumerator / (double) mDenominator;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof Rational) {
            Rational data = (Rational) obj;
            return mNumerator == data.mNumerator && mDenominator == data.mDenominator;
        }
        return false;
    }
    @Override
    public String toString() {
        return mNumerator + "/" + mDenominator;
    }
}
