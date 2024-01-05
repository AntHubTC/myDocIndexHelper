-- drop table KV_INFO;
create table if not exists KV_INFO (
    KEY varchar2(400) not null,
    VALUE varchar2(400),
    COMMENT varchar2(1000),
    primary key (`KEY`)
);
comment on column KV_INFO.KEY is '键';
comment on column KV_INFO.VALUE is '值';
comment on column KV_INFO.COMMENT is '注释';
comment on table KV_INFO is 'kv配置表';