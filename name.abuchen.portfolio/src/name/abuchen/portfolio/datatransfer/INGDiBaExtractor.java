package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class INGDiBaExtractor extends AbstractPDFExtractor
{

    public INGDiBaExtractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierabrechnung Kauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^Nominale St.ck (?<shares>\\d+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("Endbetrag zu Ihren Lasten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match("Handelsplatzgeb.hr (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Provision (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Handelsentgelt (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))
                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsgutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^Nominale (?<shares>\\d+(,\\d+)?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("Gesamtbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));

        type = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type);

        block = new Block("Zinsgutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("Gesamtbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

}
