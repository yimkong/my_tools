package rank;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class RankItem implements Comparable<RankItem> {

    /**
     * playerId
     */
    private long id;

    /**
     * value 战力
     */
    private long value;

    /**
     * addTime
     */
    private long addTime;

    /**
     * 附加属性
     */
    private long[] addition;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public long[] getAddition() {
        return addition;
    }

    public void setAddition(long[] addition) {
        this.addition = addition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RankItem other = (RankItem) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public int compareTo(RankItem o) {
        // 倒序
        CompareToBuilder compare = new CompareToBuilder().append(o.value, value).append(addTime, o.addTime);
        long[] oAddition = o.addition;
        if (ArrayUtils.isNotEmpty(oAddition) && ArrayUtils.isNotEmpty(this.addition)
                && this.addition.length == oAddition.length) {
            int length = this.addition.length;
            for (int i = 0; i < length; ++i) {
                long val = this.addition[i];
                long oVal = oAddition[i];
                compare = compare.append(oVal, val);
            }
        }
        return compare.append(o.id, id).toComparison();
    }

    @Override
    public String toString() {
        return "{id:" + id + ", value:" + value + ", addTime:" + addTime + "}";
    }

    public static RankItem valueOf(long id, int value) {
        RankItem e = new RankItem();
        e.id = id;
        e.value = value;
        return e;
    }

    public static RankItem valueOf(long id, long value, long addTime, long... addition) {
        RankItem e = new RankItem();
        e.id = id;
        e.value = value;
        e.addTime = addTime;
        if (ArrayUtils.isNotEmpty(addition)) {
            e.addition = addition;
        }


        return e;
    }

}
