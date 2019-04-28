package rank.top;

import lombok.Builder;
import lombok.Value;

/**
 * 入榜结果
 * <p>
 * Created by LeiJun on 2018/7/20.
 */
@Builder
@Value
public class TopResult {

    public static final TopResult simpleTopResultBuilder = TopResult.builder().build();

    /**
     * 旧排名
     */
    private int oldRank;
    /**
     * 新排名
     */
    private int newRank;

}
