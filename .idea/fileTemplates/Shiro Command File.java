#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end
#parse("File Header.java")

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import net.dv8tion.jda.api.JDA;

import java.util.Map;

@Command(
    name = "${NAME.replaceFirst("Command", "").toLowerCase()}",
    category = Category.${CATEGORY}
)
public class ${NAME} implements Executable {
    @Override
    public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, Map<String, String> args) {
        
    }
}
