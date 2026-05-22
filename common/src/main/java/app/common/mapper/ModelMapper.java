package app.common.mapper;

import app.common.dto.*;
import app.common.models.*;
import java.util.List;
import java.util.stream.Collectors;

public class ModelMapper {

  public static AccountDto toAccountDto(Account account) {
    if (account == null) return null;
    return new AccountDto(account.getUsername(), account.getRole());
  }

  public static Account toAccountModel(AccountDto dto) {
    if (dto == null) return null;
    return new Account(dto.username(), null, dto.role());
  }

  public static WalletDto toWalletDto(Wallet wallet) {
    if (wallet == null) return null;
    return new WalletDto(wallet.getAvailableBalance(), wallet.getFrozenFundsSnapshot());
  }

  public static Wallet toWalletModel(WalletDto dto) {
    if (dto == null) return null;
    return new Wallet(dto.availableBalance(), dto.frozenFunds());
  }

  public static UserDto toUserDto(User user) {
    if (user == null) return null;
    return new UserDto(
        user.getId(),
        user.getName(),
        toAccountDto(user.getAccount()),
        toWalletDto(user.getWallet()));
  }

  public static User toUserModel(UserDto dto) {
    if (dto == null) return null;
    if (dto.wallet() == null) {
      return User.createPublicUser(dto.id(), dto.name(), toAccountModel(dto.account()));
    }
    return new User(
        dto.id(), dto.name(), toAccountModel(dto.account()), toWalletModel(dto.wallet()));
  }

  public static BidDto toBidDto(Bid bid) {
    if (bid == null) return null;
    return new BidDto(
        bid.getId(),
        bid.getAuctionId(),
        bid.getBidderId(),
        bid.getBidderName(),
        bid.getAmount(),
        bid.getCreateAt(),
        bid.isAutoBid(),
        toUserDto(bid.getBidder()));
  }

  public static Bid toBidModel(BidDto dto) {
    if (dto == null) return null;
    Bid bid =
        new Bid(
            dto.id(),
            dto.auctionId(),
            dto.bidderId(),
            dto.bidderName(),
            dto.amount(),
            dto.createAt(),
            dto.isAutoBid());
    if (dto.bidder() != null) {
      bid.setBidder(toUserModel(dto.bidder()));
    }
    return bid;
  }

  public static ItemDto toItemDto(Item item) {
    if (item == null) return null;
    return new ItemDto(
        item.getId(),
        item.getSellerId(),
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType(),
        item.isDeleted(),
        item.getImageUrl(),
        toUserDto(item.getSeller()));
  }

  public static Item toItemModel(ItemDto dto) {
    if (dto == null) return null;
    Item item =
        ItemFactory.createItem(
            dto.id(),
            dto.name(),
            dto.sellerId(),
            dto.description(),
            dto.startingPrice(),
            dto.stepPrice(),
            dto.type());
    item.setDeleted(dto.deleted());
    item.setImageUrl(dto.imageUrl());
    if (dto.seller() != null) {
      item.setSeller(toUserModel(dto.seller()));
    }
    return item;
  }

  public static AuctionDto toAuctionDto(Auction auction) {
    if (auction == null) return null;
    List<BidDto> bids =
        auction.getBids() != null
            ? auction.getBids().stream().map(ModelMapper::toBidDto).collect(Collectors.toList())
            : null;
    return new AuctionDto(
        auction.getId(),
        auction.getItemId(),
        auction.getSellerId(),
        auction.getWinnerId(),
        auction.getStatus(),
        auction.getStartTime(),
        auction.getEndTime(),
        auction.getHighestBid(),
        auction.getExtendedCount(),
        auction.getVersion(),
        auction.getCreatedAt(),
        auction.getUpdatedAt(),
        toItemDto(auction.getItem()),
        toUserDto(auction.getSeller()),
        toUserDto(auction.getWinner()),
        bids);
  }

  public static Auction toAuctionModel(AuctionDto dto) {
    if (dto == null) return null;
    Auction auction =
        new Auction(
            dto.id(),
            dto.itemId(),
            dto.sellerId(),
            dto.winnerId(),
            dto.status(),
            dto.startTime(),
            dto.endTime(),
            dto.highestBid(),
            dto.extendedCount(),
            dto.version(),
            dto.createdAt(),
            dto.updatedAt());
    if (dto.item() != null) auction.setItem(toItemModel(dto.item()));
    if (dto.seller() != null) auction.setSeller(toUserModel(dto.seller()));
    if (dto.winner() != null) auction.setWinner(toUserModel(dto.winner()));
    if (dto.bids() != null) {
      auction.setBids(
          dto.bids().stream().map(ModelMapper::toBidModel).collect(Collectors.toList()));
    }
    return auction;
  }
}
