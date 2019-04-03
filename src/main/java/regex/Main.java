package regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class Main {
    private  static String filterEmoji(String source){
        if (source == null || source.equals("")) {
            return "";
        }
        Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher emojiMatcher = emoji.matcher(source);
        if (emojiMatcher.find()) {
            source = emojiMatcher.replaceAll("");
            return source;
        }
        return source;
    }

}
