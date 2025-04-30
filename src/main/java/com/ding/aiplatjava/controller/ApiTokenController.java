package com.ding.aiplatjava.controller;

import java.util.List; // 假设有一个用于创建的DTO
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ding.aiplatjava.dto.ApiTokenDto;
import com.ding.aiplatjava.entity.ApiToken;
import com.ding.aiplatjava.service.ApiTokenService;

import lombok.RequiredArgsConstructor;

/**
 * API Token 相关API控制器
 */
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    /**
     * 获取当前用户的所有 API Tokens (Token值保持加密状态)。
     * @return ApiToken 列表 (不含明文 Token)
     */
    @GetMapping
    public ResponseEntity<List<ApiTokenDto>> getCurrentUserTokens(/* @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码
        
        List<ApiToken> tokens = apiTokenService.getTokensByUserId(currentUserId);
        
        // 转换为 DTO，确保不暴露任何敏感信息，即使它们在实体中是加密的
        List<ApiTokenDto> tokenDtos = tokens.stream()
                .map(this::convertToDto)//意思就是将ApiToken实体转换为ApiTokenDto
                .collect(Collectors.toList());//意思就是将转换后的ApiTokenDto列表收集起来
                
        return ResponseEntity.ok(tokenDtos);
    }

    /**
     * 为当前用户创建新的 API Token。
     * @param tokenDto 包含 provider 和明文 tokenValue 的 DTO。
     * @return 创建后的 ApiToken DTO (不含明文 Token) 和 201 Created 状态。
     */
    @PostMapping
    public ResponseEntity<ApiTokenDto> createToken(@RequestBody ApiTokenDto tokenDto /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码

        // 将 DTO 转换为实体类以传递给 Service 层
        ApiToken apiToken = new ApiToken();
        apiToken.setProvider(tokenDto.getProvider());
        apiToken.setTokenValue(tokenDto.getTokenValue()); // Service 层会加密这个值

        ApiToken createdToken = apiTokenService.createToken(apiToken, currentUserId);
        
        // 将返回的实体（包含加密值和ID）转换为 DTO 再返回给前端
        ApiTokenDto createdTokenDto = convertToDto(createdToken);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTokenDto);
    }

    /**
     * 删除指定 ID 的 API Token。
     * @param id 要删除的 Token ID。
     * @return 204 No Content 或 404 Not Found。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable Long id /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码
        
        boolean deleted = apiTokenService.deleteToken(id, currentUserId);
        
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 将 ApiToken 实体转换为 ApiTokenDto。
     * 主要目的是确保返回给客户端的数据不包含 tokenValue (即使是加密的)。
     * DTO 可以只包含 id, userId, provider, createdAt, updatedAt 等安全信息。
     */
    private ApiTokenDto convertToDto(ApiToken apiToken) {
        ApiTokenDto dto = new ApiTokenDto();
        dto.setId(apiToken.getId());
        dto.setUserId(apiToken.getUserId());
        dto.setProvider(apiToken.getProvider());
        // dto.setTokenValue(null); // 明确不设置或设为null
        dto.setCreatedAt(apiToken.getCreatedAt());
        dto.setUpdatedAt(apiToken.getUpdatedAt());
        return dto;
    }

    // 注意：通常不提供直接获取解密 Token 的 API 端点。
    // 如果确实需要（例如，前端需要临时获取 Token 用于某项操作），
    // 必须设计非常谨慎的、临时的、有严格权限控制的 API，
    // 或者考虑让后端直接代理对 AI 服务的调用，而不是将 Token 发送给前端。
} 