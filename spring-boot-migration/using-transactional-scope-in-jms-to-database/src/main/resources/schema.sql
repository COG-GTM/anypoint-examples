-- order.sql from the Mule app, made idempotent for spring.sql.init
CREATE TABLE IF NOT EXISTS orders (
  id int(11) NOT NULL AUTO_INCREMENT,
  item_id int(11) NOT NULL,
  item_units int(11) NOT NULL,
  customer_id int(11) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8;
