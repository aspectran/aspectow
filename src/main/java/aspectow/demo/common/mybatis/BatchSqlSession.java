package aspectow.demo.common.mybatis;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlSessionAgent;
import org.apache.ibatis.session.ExecutorType;

@Component
@Bean(id = "batchSqlSession", lazyDestroy = true)
public class BatchSqlSession extends SqlSessionAgent {

    public BatchSqlSession() {
        super("batchTxAspect");
        setExecutorType(ExecutorType.BATCH);
        setAutoParameters(true);
    }

}
