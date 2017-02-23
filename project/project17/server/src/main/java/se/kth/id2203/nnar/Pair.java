package se.kth.id2203.nnar;

public class Pair {

    private int ts;
    private int wr;

    public Pair(int ts, int wr) {
        this.ts = ts;
        this.wr = wr;
    }

    public int getTs() {
        return ts;
    }

    public void setTs(int ts) {
        this.ts = ts;
    }

    public int getWr() {
        return wr;
    }

    public void setWr(int wr) {
        this.wr = wr;
    }

    /**
     * Checks if pair one is bigger then pair two
     * @param pairOne
     * @param pairTwo
     * @return
     */
    public boolean biggerThan(Pair pairOne, Pair pairTwo) {

        if (pairOne.getTs() > pairTwo.getTs()) {

            if (pairOne.getWr() > pairTwo.getWr()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (ts != pair.ts) return false;
        return wr == pair.wr;
    }

    @Override
    public int hashCode() {
        int result = ts;
        result = 31 * result + wr;
        return result;
    }
}
