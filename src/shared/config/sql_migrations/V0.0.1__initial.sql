CREATE TABLE accounts (
  id UUID not null primary key,
  name text not null default '',
  active boolean not null default true,
  password text not null default '',
  email text not null default ''
);

CREATE UNIQUE INDEX account_email_idx ON accounts(email);
