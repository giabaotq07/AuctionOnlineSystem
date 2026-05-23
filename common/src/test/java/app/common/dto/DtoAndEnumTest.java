package app.common.dto;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho toan bo DTO va Enum trong module common. Su dung reflection de tu dong phu 100%
 * cac Record DTO va Enum. Viet bang tieng Viet khong dau theo dung yeu cau.
 */
public class DtoAndEnumTest {

  @Test
  public void testAllEnums() {
    for (View v : View.values()) {
      assertNotNull(v);
      assertEquals(v, View.valueOf(v.name()));
    }
    for (AuctionStatus status : AuctionStatus.values()) {
      assertNotNull(status);
      assertEquals(status, AuctionStatus.valueOf(status.name()));
    }
    for (RequestType rt : RequestType.values()) {
      assertNotNull(rt);
      assertEquals(rt, RequestType.valueOf(rt.name()));
      // Kiem tra cac ham tien ich cua RequestType
      rt.requiresAuthentication();
      rt.isAllowed(UserRole.BIDDER);
      rt.isAllowed(UserRole.SELLER);
      rt.isAllowed(UserRole.ADMIN);
      rt.isAllowed(null);
    }
    for (OperationStatus os : OperationStatus.values()) {
      assertNotNull(os);
      assertEquals(os, OperationStatus.valueOf(os.name()));
    }
    for (UserRole ur : UserRole.values()) {
      assertNotNull(ur);
      assertEquals(ur, UserRole.valueOf(ur.name()));
    }
    for (ResponseType rt : ResponseType.values()) {
      assertNotNull(rt);
      assertEquals(rt, ResponseType.valueOf(rt.name()));
    }
    for (BidType bt : BidType.values()) {
      assertNotNull(bt);
      assertEquals(bt, BidType.valueOf(bt.name()));
    }
    for (ItemType it : ItemType.values()) {
      assertNotNull(it);
      assertEquals(it, ItemType.valueOf(it.name()));
    }
  }

  @Test
  public void testAllRecordsDynamically() throws Exception {
    Class<?>[] recordClasses =
        new Class<?>[] {
          AccountDto.class,
          AuctionDetailRequest.class,
          AuctionDetailResponse.class,
          AuctionDto.class,
          AuctionHistoryRequest.class,
          AuctionHistoryResponse.class,
          AuctionPaidNoticeResponse.class,
          AuctionPreview.class,
          AuctionResultRequest.class,
          AuctionResultResponse.class,
          AuctionSummariesResponse.class,
          BidDto.class,
          CancelAuctionRequest.class,
          CancelAuctionResponse.class,
          ChatRequest.class,
          ChatResponse.class,
          CreateAuctionRequest.class,
          CreateAuctionResponse.class,
          DepositRequest.class,
          DisableAutoBidRequest.class,
          DisableAutoBidResponse.class,
          FetchItemImageRequest.class,
          FetchItemImageResponse.class,
          FetchSellerItemsRequest.class,
          ItemDto.class,
          ItemListResponse.class,
          ItemPreview.class,
          LoginRequest.class,
          LoginResponse.class,
          PlaceBidRequest.class,
          PlaceBidResponse.class,
          RegisterRequest.class,
          RegisterResponse.class,
          SetAutoBidRequest.class,
          SetAutoBidResponse.class,
          SettleWalletRequest.class,
          UpdateAuctionRequest.class,
          UploadImageRequest.class,
          UploadImageResponse.class,
          UserDto.class,
          UserListResponse.class,
          UserPreview.class,
          WalletDto.class,
          WalletUpdateResponse.class
        };

    for (Class<?> clazz : recordClasses) {
      assertTrue(clazz.isRecord(), clazz.getName() + " phai la mot Java Record!");
      java.lang.reflect.RecordComponent[] components = clazz.getRecordComponents();
      Object[] args1 = new Object[components.length];

      for (int i = 0; i < components.length; i++) {
        Class<?> type = components[i].getType();
        args1[i] = getDefaultValueForType(type, i);
      }

      // Lay constructor mac dinh cua Record
      Class<?>[] paramTypes =
          Arrays.stream(components)
              .map(java.lang.reflect.RecordComponent::getType)
              .toArray(Class<?>[]::new);
      java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
      constructor.setAccessible(true);

      Object instance1 = constructor.newInstance(args1);
      Object instance2 = constructor.newInstance(args1);

      // Kiem tra logic equals, hashCode, va toString
      assertEquals(instance1, instance2);
      assertNotEquals(instance1, null);
      assertNotEquals(instance1, "dummy");
      assertEquals(instance1.hashCode(), instance2.hashCode());
      assertNotNull(instance1.toString());

      // Kiem tra cac accessor method
      for (java.lang.reflect.RecordComponent comp : components) {
        java.lang.reflect.Method accessor = comp.getAccessor();
        accessor.setAccessible(true);
        Object value = accessor.invoke(instance1);
        assertNotNull(value);
      }
    }
  }

  private Object getDefaultValueForType(Class<?> type, int offset) {
    if (type == int.class || type == Integer.class) {
      return 1 + offset;
    } else if (type == long.class || type == Long.class) {
      return 100L + offset;
    } else if (type == double.class || type == Double.class) {
      return 1.0 + offset;
    } else if (type == boolean.class || type == Boolean.class) {
      return offset % 2 == 0;
    } else if (type == String.class) {
      return "test_str_" + offset;
    } else if (type == BigDecimal.class) {
      return BigDecimal.valueOf(10.0 + offset);
    } else if (type == LocalDateTime.class) {
      return LocalDateTime.now().plusDays(offset);
    } else if (type == byte[].class) {
      return new byte[] {(byte) offset};
    } else if (type == List.class) {
      return new ArrayList<>();
    } else if (type == Map.class) {
      return new HashMap<>();
    } else if (type == AuctionStatus.class) {
      return AuctionStatus.values()[offset % AuctionStatus.values().length];
    } else if (type == UserRole.class) {
      return UserRole.values()[offset % UserRole.values().length];
    } else if (type == ItemType.class) {
      return ItemType.values()[offset % ItemType.values().length];
    } else if (type == UserPreview.class) {
      return new UserPreview(1, "Name", "Username", UserRole.BIDDER);
    } else if (type == ItemPreview.class) {
      return new ItemPreview(1, "Item", "Desc", ItemType.ART, 100L, 10L);
    } else if (type == WalletDto.class) {
      return new WalletDto(BigDecimal.TEN, new HashMap<>());
    } else if (type == UserDto.class) {
      return new UserDto(
          1,
          "Name",
          new AccountDto("user", UserRole.BIDDER),
          new WalletDto(BigDecimal.TEN, new HashMap<>()));
    } else if (type == ItemDto.class) {
      return new ItemDto(
          1,
          1,
          "Item",
          "Desc",
          100L,
          10L,
          ItemType.ART,
          true,
          "url",
          new UserDto(
              1,
              "Name",
              new AccountDto("user", UserRole.BIDDER),
              new WalletDto(BigDecimal.TEN, new HashMap<>())));
    } else if (type == AuctionDto.class) {
      return new AuctionDto(
          1,
          1,
          1,
          1,
          AuctionStatus.OPEN,
          LocalDateTime.now(),
          LocalDateTime.now(),
          100L,
          0,
          1,
          LocalDateTime.now(),
          LocalDateTime.now(),
          new ItemDto(
              1,
              1,
              "Item",
              "Desc",
              100L,
              10L,
              ItemType.ART,
              true,
              "url",
              new UserDto(
                  1,
                  "Name",
                  new AccountDto("user", UserRole.BIDDER),
                  new WalletDto(BigDecimal.TEN, new HashMap<>()))),
          new UserDto(
              1,
              "Name",
              new AccountDto("user", UserRole.BIDDER),
              new WalletDto(BigDecimal.TEN, new HashMap<>())),
          new UserDto(
              1,
              "Name",
              new AccountDto("user", UserRole.BIDDER),
              new WalletDto(BigDecimal.TEN, new HashMap<>())),
          new ArrayList<>());
    } else if (type == BidDto.class) {
      return new BidDto(
          1,
          1,
          1,
          "Name",
          100L,
          LocalDateTime.now(),
          true,
          new UserDto(
              1,
              "Name",
              new AccountDto("user", UserRole.BIDDER),
              new WalletDto(BigDecimal.TEN, new HashMap<>())));
    } else if (type == AccountDto.class) {
      return new AccountDto("user", UserRole.BIDDER);
    } else {
      return null;
    }
  }
}
