# drop table user_login_hist;
create table guests
(
    user_no    int auto_increment comment '사용자 번호' primary key,
    user_nm    varchar(100) not null comment '사용자명',
    country_cd varchar(5) null comment '국가코드',
    lang_cd varchar(5) null comment '언어코드',
    ip_addr    varchar(50) null comment 'IP 주소',
    discarded_dt  datetime null comment '사용자명 폐기일',
    reg_dt     datetime default now() not null comment '등록일자'
)
comment '게스트 사용자명' engine=MyISAM charset=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table rooms
(
    room_id    int auto_increment comment '대화방 일련번호' primary key,
    room_nm    varchar(100) not null comment '대화방 이름',
    lang_cd    varchar(10) not null comment '언어 코드 (en, ko)',
    user_no    int not null comment '사용자 번호(만든이)',
    cumu_users int default 0 not null comment '누적 이용자 수',
    curr_users int default 0 not null comment '현재 이용자 수',
    used_dt    datetime default now() not null comment '최근 사용일',
    del_yn     char(1) default 'N' not null comment '삭제 여부',
    reg_dt     datetime default now() not null comment '등록일',
    upd_dt     datetime default now() not null comment '수정일'
)
comment '대화방 마스터' engine=MyISAM charset=utf8mb4 COLLATE=utf8mb4_unicode_ci;
