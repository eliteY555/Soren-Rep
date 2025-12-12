package com.tailai.common.client;

import com.tailai.common.dto.FileUploadResponse;
import com.tailai.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件服务统一客户端
 * 供所有微服务调用 tailai-file-service
 *
 * @author Tailai
 */
@Slf4j
@Component
public class FileServiceClient {

    private final RestTemplate restTemplate;

    @Value("${file-service.url:http://localhost:8090}")
    private String fileServiceUrl;

    public FileServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 通用文件上传
     */
    public Result<FileUploadResponse> uploadFile(MultipartFile file, String directory) {
        try {
            log.info("调用文件服务上传文件，文件名：{}，目录：{}", file.getOriginalFilename(), directory);

            String url = fileServiceUrl + "/api/file/upload?directory=" + directory;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Result<FileUploadResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<FileUploadResponse>>() {}
            );

            return handleResponse(response);

        } catch (Exception e) {
            log.error("调用文件服务失败", e);
            return Result.error(500, "文件服务调用失败：" + e.getMessage());
        }
    }

    /**
     * 合同模板上传
     */
    public Result<FileUploadResponse> uploadTemplate(MultipartFile file) {
        try {
            log.info("调用文件服务上传合同模板，文件名：{}", file.getOriginalFilename());

            String url = fileServiceUrl + "/api/file/template/upload";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Result<FileUploadResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<FileUploadResponse>>() {}
            );

            return handleResponse(response);

        } catch (Exception e) {
            log.error("上传模板失败", e);
            return Result.error(500, "上传模板失败：" + e.getMessage());
        }
    }

    /**
     * 上传证件图片
     */
    public Result<FileUploadResponse> uploadImage(MultipartFile file, String imageType) {
        try {
            log.info("调用文件服务上传图片，文件名：{}，类型：{}", file.getOriginalFilename(), imageType);

            String url = fileServiceUrl + "/api/file/image/upload?imageType=" + imageType;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Result<FileUploadResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<FileUploadResponse>>() {}
            );

            return handleResponse(response);

        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.error(500, "上传图片失败：" + e.getMessage());
        }
    }

    /**
     * 上传文件到自定义路径
     */
    public Result<FileUploadResponse> uploadFileCustomPath(MultipartFile file, String customPath) {
        try {
            log.info("调用文件服务上传文件到自定义路径，文件名：{}，路径：{}", file.getOriginalFilename(), customPath);

            String url = fileServiceUrl + "/api/file/upload-custom?customPath=" + customPath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Result<FileUploadResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<FileUploadResponse>>() {}
            );

            return handleResponse(response);

        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Result.error(500, "上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public Result<String> deleteFile(String ossPath) {
        try {
            log.info("删除文件，OSS路径：{}", ossPath);

            String url = fileServiceUrl + "/api/file/delete?ossPath=" + ossPath;

            ResponseEntity<Result<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Result<String>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.error(500, "文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件临时访问URL
     */
    public Result<String> getDownloadUrl(String ossPath, Integer expireMinutes) {
        try {
            log.info("获取文件临时URL，OSS路径：{}，过期时间：{}分钟", ossPath, expireMinutes);

            String url = fileServiceUrl + "/api/file/download-url?ossPath=" + ossPath + "&expireMinutes=" + expireMinutes;

            ResponseEntity<Result<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<String>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("获取临时URL失败", e);
            return Result.error(500, "获取临时URL失败：" + e.getMessage());
        }
    }

    /**
     * 生成合同PDF
     */
    public Result<Map<String, Object>> generateContractPdf(
            String templateOssPath,
            String outputPath,
            Map<String, Object> contractData
    ) {
        try {
            log.info("调用文件服务生成合同PDF，模板路径：{}，输出路径：{}", templateOssPath, outputPath);

            String url = fileServiceUrl + "/api/file/contract/generate-pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("templateOssPath", templateOssPath);
            requestBody.put("outputPath", outputPath);
            requestBody.put("contractData", contractData);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("生成合同PDF失败", e);
            return Result.error(500, "生成合同PDF失败：" + e.getMessage());
        }
    }

    /**
     * 添加签名到PDF
     */
    public Result<Map<String, Object>> addSignatureToPdf(
            String pdfPath,
            String signatureBase64,
            String employeeName,
            String signerName,
            String signerRole
    ) {
        try {
            log.info("调用文件服务添加签名到PDF，PDF路径：{}，签名人：{}，角色：{}", pdfPath, signerName, signerRole);

            String url = fileServiceUrl + "/api/file/signature/add";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("pdfPath", pdfPath);
            requestBody.put("signatureBase64", signatureBase64);
            requestBody.put("employeeName", employeeName);
            requestBody.put("signerName", signerName);
            requestBody.put("signerRole", signerRole);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("添加签名失败", e);
            return Result.error(500, "添加签名失败：" + e.getMessage());
        }
    }

    /**
     * 列出指定目录下的文件
     */
    public Result<Map<String, Object>> listFiles(String folderPath) {
        try {
            log.info("列出文件，文件夹路径：{}", folderPath);

            String url = fileServiceUrl + "/api/file/list?folderPath=" + folderPath;

            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("列出文件失败", e);
            return Result.error(500, "列出文件失败：" + e.getMessage());
        }
    }

    /**
     * 根据员工姓名列出文件
     */
    public Result<Map<String, Object>> listFilesByEmployeeName(String employeeName) {
        try {
            log.info("根据员工姓名列出文件，员工姓名：{}", employeeName);

            String url = fileServiceUrl + "/api/file/list-by-employee?employeeName=" + employeeName;

            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("列出文件失败", e);
            return Result.error(500, "列出文件失败：" + e.getMessage());
        }
    }

    /**
     * 处理响应
     */
    private Result<FileUploadResponse> handleResponse(ResponseEntity<Result<FileUploadResponse>> response) {
        Result<FileUploadResponse> result = response.getBody();
        if (result == null) {
            log.error("文件服务返回结果为空");
            return Result.error(500, "文件服务返回结果为空");
        }

        if (result.getCode() != 200) {
            log.error("文件服务操作失败：{}", result.getMessage());
            return Result.error(result.getCode(), result.getMessage());
        }

        return result;
    }

    /**
     * MultipartFile资源包装类
     */
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile file) throws Exception {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}

