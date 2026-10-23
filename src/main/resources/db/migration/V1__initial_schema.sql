create table regions (
    id bigserial primary key,
    name varchar(40) not null,
    slug varchar(80) not null,
    display_order integer not null,
    enabled boolean not null default true,
    constraint uk_region_slug unique (slug)
);

create table neighborhoods (
    id bigserial primary key,
    region_id bigint not null references regions(id),
    name varchar(40) not null,
    slug varchar(80) not null,
    display_order integer not null,
    enabled boolean not null default true,
    constraint uk_neighborhood_region_slug unique (region_id, slug)
);

create table moving_services (
    id bigserial primary key,
    name varchar(40) not null,
    slug varchar(80) not null,
    short_description varchar(180) not null,
    display_order integer not null,
    enabled boolean not null default true,
    constraint uk_service_slug unique (slug)
);

create table landing_contents (
    id bigserial primary key,
    region_id bigint not null references regions(id),
    neighborhood_id bigint references neighborhoods(id),
    service_id bigint not null references moving_services(id),
    seo_title varchar(120),
    meta_description varchar(200),
    headline varchar(300),
    introduction varchar(1200),
    body text,
    published boolean not null default false,
    updated_at timestamp with time zone not null
);

create index idx_landing_region_service on landing_contents(region_id, service_id);
create index idx_landing_neighborhood on landing_contents(neighborhood_id);

create table site_settings (
    id bigint primary key,
    business_name varchar(60) not null,
    phone_display varchar(30) not null,
    phone_href varchar(30) not null,
    kakao_url varchar(200) not null,
    tagline varchar(100) not null
);
