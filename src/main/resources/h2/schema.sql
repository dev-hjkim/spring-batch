DROP TABLE IF EXISTS Customer2;
CREATE TABLE Customer2(
    id INT PRIMARY KEY  AUTO_INCREMENT,
    first_name VARCHAR(45),
    middle_initial VARCHAR(1),
    last_name VARCHAR(45),
    address VARCHAR(45),
    city VARCHAR(45),
    state VARCHAR(2),
    zip VARCHAR(5)
);
