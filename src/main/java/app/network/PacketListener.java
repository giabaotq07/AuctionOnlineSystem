package app.network;

public interface PacketListener<T> {
    void handle(T packet);
}
