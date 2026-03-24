package savage.commoneconomy.model;

import java.math.BigDecimal;

/**
 * Represents a player's economy account data.
 * This is a plain data object used for serialization and caching.
 */
public class AccountData {
    private String name;
    private BigDecimal balance;
    private long version; // For optimistic locking

    public AccountData(String name, BigDecimal balance) {
        this(name, balance, 0L);
    }

    public AccountData(String name, BigDecimal balance, long version) {
        this.name = name;
        this.balance = balance;
        this.version = version;
    }

    // Getters
    public String getName() { return name; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setVersion(long version) { this.version = version; }

    /**
     * Increments the version for optimistic locking.
     */
    public void incrementVersion() {
        this.version++;
    }
}
