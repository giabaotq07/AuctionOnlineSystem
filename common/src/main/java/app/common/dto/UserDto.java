package app.common.dto;

public record UserDto(
    int id, String name, AccountDto account, WalletDto wallet, String avatarUrl, boolean isBanned) {
  public UserDto(int id, String name, AccountDto account, WalletDto wallet, String avatarUrl) {
    this(id, name, account, wallet, avatarUrl, false);
  }
}
