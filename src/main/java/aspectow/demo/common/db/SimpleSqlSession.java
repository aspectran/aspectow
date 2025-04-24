package aspectow.demo.common.db;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlSessionAgent;

@Component
@Bean(id = "simpleSqlSession", lazyDestroy = true, proxied = true)
public class SimpleSqlSession extends SqlSessionAgent {

    public SimpleSqlSession() {
        super("simpleTxAspect");
        setAutoParameters(true);
    }

}
