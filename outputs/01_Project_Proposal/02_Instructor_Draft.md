# Credit Card Transaction Simulator

**Capstone project idea**<br>
**Presentation:** October 13, 2026

## The idea

I want to build a small web app that simulates how a credit card purchase moves through a banking system. A customer would sign in, use a fictional test card to submit a purchase, and immediately see whether it was approved or declined. The app would update the account's available credit, save the transaction, and show it in a history view. Customers could also request a full refund. An admin could review activity and freeze or reactivate an account.

This connects with the credit card transaction work I'll be seeing on the Capital One project. I want the app to show both the customer experience and the rules behind a transaction, while keeping the scope small enough to build and explain well.

## What I plan to build

- A React interface with a dashboard, purchase form, transaction history, and admin view.
- A Spring Boot API and MySQL database for users, fictional cards, credit accounts, and transactions.
- Customer and admin access, with customers limited to their own accounts.
- Passwords stored with BCrypt and signed tokens for protected requests.
- Purchase checks for card-field format, positive amounts, available credit, and frozen accounts. The server will perform the checks even when the form has already checked them.
- A full-refund action that can be used once for an approved purchase.
- Protection against accidentally submitting the same purchase twice, if the core flow is working on schedule.

I also want to make the card form more engaging with a **flippable 3D virtual card**. The card would show masked test details as the user fills out the form. The regular form would still work without the animation. My goal is to finish the purchase flow first, then spend a dedicated day on the 3D interaction before the October 12 trial presentation.

## Demo and boundaries

The demo would show a successful purchase, a decline when available credit is too low, the updated history, a refund, and an admin freezing the account. If duplicate-submission protection is ready, I would also retry the same request and show that it does not create a second charge.

This is a **simulation**, with fictional users, card numbers, and balances. It will not connect to a real bank or payment processor. I will not store full card numbers or CVV values. AWS implementation and a Jira board are outside my plan based on your guidance.

Does this sound like the right scope and direction for the capstone? I would especially appreciate your take on whether the purchase/refund flow and 3D card make a strong enough demo for the time available.
