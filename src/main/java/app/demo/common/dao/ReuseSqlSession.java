package app.demo.common.dao;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlSessionAgent;

@Component
@Bean("reuseSqlSession")
public class ReuseSqlSession extends SqlSessionAgent {

    public ReuseSqlSession() {
        super("reuseTxAspect");
        setAutoParameters(true);
    }

}
