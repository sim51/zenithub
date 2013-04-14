# --- First database schema

# --- !Ups

create sequence user_seq start with 1000;
create table user (
  id bigint not null primary key,
  firstname varchar(255),
  lastname varchar(255),
  fullname varchar(255),
  email varchar(255),
  avatarUrl varchar(255)
);

create table userAccount (
  id varchar(255) not null primary key,
  provider varchar(255) not null,
  json text,
  user_id bigint not null,
  foreign key(user_id) references user(id) on delete cascade,
);

# --- !Downs

drop table if exists userAccount;
drop table if exists user;
drop sequence if exists user_seq;
