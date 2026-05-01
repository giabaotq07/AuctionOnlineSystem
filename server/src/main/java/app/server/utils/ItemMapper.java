package app.server.utils;

import app.enums.ItemCategory;
import app.models.Art;
import app.models.Electronics;
import app.models.Item;
import app.models.Vehicle;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ItemMapper {
  private ItemMapper() {}

  public static Item map(ResultSet rs) throws SQLException {
  ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
  int id = rs.getInt("id");
  String name = rs.getString("name");
  String description = rs.getString("description");
  double startPrice = rs.getDouble("start_price");
  double stepPrice = rs.getDouble("step_price");

  return switch (category) {
   case ELECTRONICS ->
    new Electronics(
  id,
  name,
  description,
  startPrice,
  stepPrice,
  rs.getString("brand"),
  rs.getString("condition"));
   case ART ->
    new Art(
  id,
  name,
  description,
  startPrice,
  stepPrice,
  rs.getString("artist"),
  rs.getInt("year"));
   case VEHICLE ->
    new Vehicle(
  id,
  name,
  description,
  startPrice,
  stepPrice,
  rs.getString("vin"),
  rs.getInt("vehicle_year"));
  };
  }
}

