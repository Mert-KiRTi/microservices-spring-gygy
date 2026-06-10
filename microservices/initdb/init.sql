-- Initialize products table and create publication for Debezium

CREATE TABLE IF NOT EXISTS public.products (
  id BIGINT PRIMARY KEY,
  name TEXT,
  price NUMERIC(19,2),
  stock INTEGER,
  description TEXT
);

-- create a publication so pgoutput can stream changes
CREATE PUBLICATION dbz_publication FOR TABLE public.products;

-- insert a sample row
INSERT INTO public.products (id, name, price, stock, description) VALUES (1, 'Initial Product', 9.99, 100, 'Seed row');
