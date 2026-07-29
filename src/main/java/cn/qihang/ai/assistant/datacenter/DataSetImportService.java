package cn.qihang.ai.assistant.datacenter;

import cn.qihang.ai.assistant.datacenter.model.DataSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 数据集导入服务 — Excel 导入已停用（Apache POI 依赖过重）
 * 建议用户改用 CSV 导入，或手动创建数据集后通过 API 录入数据。
 */
@Service
public class DataSetImportService {

    private static final Logger log = LoggerFactory.getLogger(DataSetImportService.class);

    public static class ExcelPreview {
        private List<String> headers;
        private List<List<String>> rows;
        private int totalRows;

        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> rows) { this.rows = rows; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    }

    /**
     * 功能已停用：Excel 预览
     * 抛异常让前端友好提示用户改用 CSV 导入
     */
    public ExcelPreview previewExcel(MultipartFile file) throws Exception {
        throw new UnsupportedOperationException(
                "Excel 导入已停用，请使用 CSV 格式导入数据");
    }

    /**
     * 功能已停用：Excel 导入（手动映射列）
     */
    public List<Map<String, Object>> importExcel(MultipartFile file, Map<String, String> columnMapping) throws Exception {
        throw new UnsupportedOperationException(
                "Excel 导入已停用，请使用 CSV 格式导入数据");
    }

    /**
     * 功能已停用：Excel 导入（自动检测列）
     */
    public List<Map<String, Object>> importExcelWithAutoDetect(MultipartFile file, DataSchema schema) throws Exception {
        throw new UnsupportedOperationException(
                "Excel 导入已停用，请使用 CSV 格式导入数据");
    }
}
