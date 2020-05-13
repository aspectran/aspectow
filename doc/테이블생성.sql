# drop table user_login_hist;
create table user_login_hist
(
    user_no    int auto_increment comment '사용자 일련번호' primary key,
    username   varchar(100) not null comment '사용자명',
    reg_date   datetime default now() not null comment '등록일자'
)
comment '사용자 로그인 내역' engine=MyISAM charset=utf8mb4 COLLATE=utf8mb4_unicode_ci;

