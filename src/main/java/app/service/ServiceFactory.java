package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.database.TransactionManager;

/**
 * Simple factory to create service instances with shared dependencies. This helps avoid duplicated
 * TransactionManager creation and makes unit testing easier because a test can provide mock
 * implementations.
 */
public class ServiceFactory {
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;

  public ServiceFactory() {
    this.transactionManager = new TransactionManager();
    this.bidValidator = new BidValidator();
    this.antiSnipeService = new AntiSnipeService();
  }

  // Constructor for tests to inject mocks
  public ServiceFactory(
      TransactionManager transactionManager,
      BidValidator bidValidator,
      AntiSnipeService antiSnipeService) {
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
    this.antiSnipeService = antiSnipeService;
  }

  public ItemService createItemService(ItemDAO itemDAO) {
    return new ItemService(itemDAO, transactionManager);
  }

  public BidService createBidService(BidDAO bidDAO, AuctionDAO auctionDAO) {
    return new BidService(bidDAO, auctionDAO, transactionManager, bidValidator, antiSnipeService);
  }

  public AuctionService createAuctionService(AuctionDAO auctionDAO, BidDAO bidDAO, ItemDAO itemDAO) {
    return new AuctionService(auctionDAO, bidDAO, itemDAO, transactionManager);
  }
}
