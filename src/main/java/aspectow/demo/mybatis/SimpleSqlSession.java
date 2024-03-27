package aspectow.demo.mybatis;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlSessionAgent;

@Component
@Bean("simpleSqlSession")
public class SimpleSqlSession extends SqlSessionAgent {

    public SimpleSqlSession() {
        super("simpleTxAspect");
        setAutoParameters(true);
    }

}
