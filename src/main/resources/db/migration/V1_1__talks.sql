CREATE TABLE IF NOT EXISTS talks(
  id    VARCHAR(64)  NOT NULL,
  title VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

INSERT
  INTO talks (id, title)
  VALUES 
    ('single-rating', 'Test a single rating'),
    ('multiple-ratings', 'Test multiple ratings')
  ON CONFLICT do nothing;

INSERT
  INTO talks (id, title)
  VALUES ('flight-of-the-flux', 'A look at Reactor execution model')
  ON CONFLICT do nothing;