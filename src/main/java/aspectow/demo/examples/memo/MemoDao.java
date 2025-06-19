/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package aspectow.demo.examples.memo;

import aspectow.demo.common.mybatis.SimpleSqlSession;
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
