create table daily_visit_stats (
    visit_date date not null,
    scope varchar(80) not null,
    page_views bigint not null default 0,
    unique_visitors bigint not null default 0,
    primary key (visit_date, scope)
);

create table daily_visit_uniques (
    visit_date date not null,
    scope varchar(80) not null,
    visitor_hash char(64) not null,
    primary key (visit_date, scope, visitor_hash)
);

create index idx_daily_visit_stats_scope_date on daily_visit_stats(scope, visit_date);
create index idx_daily_visit_uniques_date on daily_visit_uniques(visit_date);
