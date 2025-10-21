package alibaba.datafilter.config;

import alibaba.datafilter.model.em.FileStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(FileStatus.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class FileStatusTypeHandler implements TypeHandler<FileStatus> {

    @Override
    public void setParameter(PreparedStatement ps, int i, FileStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public FileStatus getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return getFileStatusFromString(value);
    }

    @Override
    public FileStatus getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return getFileStatusFromString(value);
    }

    @Override
    public FileStatus getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return getFileStatusFromString(value);
    }

    private FileStatus getFileStatusFromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (FileStatus status : FileStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        
        // 如果数据库中的值无法匹配任何枚举值，则抛出更明确的异常或返回默认值
        throw new IllegalArgumentException("Unknown FileStatus value: " + value);
    }
}