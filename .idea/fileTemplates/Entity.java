#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end
#parse("File Header.java")

import com.kuuhaku.controller.DAO;

import javax.persistence.*;

@Entity
@Table(name = "${NAME.toLowerCase()}")
public class ${NAME} extends DAO {
    
}
