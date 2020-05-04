package club.textchat.common.mybatis;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlSessionAgent;

@Component
@Bean("batchSqlSession")
public class BatchSqlSession extends SqlSessionAgent {

    public BatchSqlSession() {
        super("batchTxAspect");
        setAutoParameters(true);
    }

}
