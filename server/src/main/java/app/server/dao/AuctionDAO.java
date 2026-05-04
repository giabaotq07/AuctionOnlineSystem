package app.server.dao;

import app.models.Auction;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
  int create(Auction auction);

  Optional<Auction> findById(int id);

  List<Auction> findAll();

  List<Auction> findLive();

  void updateStatus(int auctionId, String status);
}

