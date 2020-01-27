package app.demo.examples.memo;

import app.demo.common.dao.BatchSqlSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import org.apache.ibatis.session.SqlSession;

@Component
@Bean("memoBatchDao")
public class MemoBatchDao {

    private final SqlSession sqlSession;

    @Autowired
    public MemoBatchDao(BatchSqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public void insertBulkMemo() {
        sqlSession.insert("memo.insertMemo");
    }

}
