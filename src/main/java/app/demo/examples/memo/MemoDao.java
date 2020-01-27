package app.demo.examples.memo;

import app.demo.common.dao.SimpleSqlSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Map;

@Component
@Bean("memoDao")
public class MemoDao {

    private final SqlSession sqlSession;

    @Autowired
    public MemoDao(SimpleSqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public Map<String, ?> getMemo() {
        return sqlSession.selectOne("memo.selectMemo");
    }

    public List<?> getList() {
        return sqlSession.selectList("memo.selectMemoList");
    }

    public void insertMemo() {
        sqlSession.insert("memo.insertMemo");
    }

    public boolean deleteMemo() {
        return (sqlSession.delete("memo.deleteMemo") > 0);
    }

    public int deleteAllMemo() {
        return sqlSession.delete("memo.deleteAllMemo");
    }

}
