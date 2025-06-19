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

import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

import java.util.List;
import java.util.Map;

@Component
@Bean("memoActivity")
public class MemoActivity {

    private final MemoDao memoDao;

    private final MemoBatchDao memoBatchDao;

    @Autowired
    public MemoActivity(MemoDao memoDao, MemoBatchDao memoBatchDao) {
        this.memoDao = memoDao;
        this.memoBatchDao = memoBatchDao;
    }

    public List<?> getList() {
        return memoDao.getList();
    }

    public Map<String, ?> addMemo() {
        memoDao.insertMemo();
        return memoDao.getMemo();
    }

    public boolean delMemo() {
        return memoDao.deleteMemo();
    }

    public int delAllMemo() {
        return memoDao.deleteAllMemo();
    }

    public int addBulkMemo(Translet translet) {
        int repetitions = Integer.parseInt(translet.getParameter("repetitions"));
        int affected = 0;
        for (int i = 0; i < repetitions; i++) {
            memoBatchDao.insertBulkMemo();
            affected++;
        }
        return affected;
    }

}
