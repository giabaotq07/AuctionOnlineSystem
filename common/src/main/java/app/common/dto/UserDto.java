package app.common.dto;

public record UserDto(
    int id, String name, AccountDto account, WalletDto wallet, String avatarUrl) {}
