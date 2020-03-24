CREATE TABLE account (
  id UUID not null primary key,
  name char(200) not null default '',
  active boolean not null default true,
  password char(256),
  password_salt char(256),
  password_iterations integer,
  email char(256) not null);

CREATE UNIQUE INDEX account_email_idx ON account(email);
