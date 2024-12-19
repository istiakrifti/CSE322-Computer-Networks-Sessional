/*
 * Copyright (c) 2016 ResiliNets, ITTC, University of Kansas
 *
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * Author: Truc Anh N. Nguyen <annguyen@ittc.ku.edu>
 *
 * James P.G. Sterbenz <jpgs@ittc.ku.edu>, director
 * ResiliNets Research Group  https://resilinets.org/
 * Information and Telecommunication Technology Center (ITTC)
 * and Department of Electrical Engineering and Computer Science
 * The University of Kansas Lawrence, KS USA.
 */

#ifndef TcpVegasModified_H
#define TcpVegasModified_H

#include "tcp-congestion-ops.h"

namespace ns3
{

class TcpSocketState;

/**
 * \ingroup congestionOps
 *
 * \brief An implementation of TCP Vegas-A (Adaptive)
 *
 * TCP Vegas-A extends TCP Vegas by introducing adaptive thresholds
 * for controlling congestion. Rather than using fixed alpha and beta values,
 * Vegas-A dynamically adjusts these thresholds based on the network
 * conditions observed during runtime.
 *
 */

class TcpVegasModified : public TcpNewReno
{
  public:
    /**
     * \brief Get the type ID.
     * \return the object TypeId
     */
    static TypeId GetTypeId();

    /**
     * Create an unbound tcp socket.
     */
    TcpVegasModified();

    /**
     * \brief Copy constructor
     * \param sock the object to copy
     */
    TcpVegasModified(const TcpVegasModified& sock);
    ~TcpVegasModified() override;

    std::string GetName() const override;

    /**
     * \brief Compute RTTs needed to execute Vegas-A algorithm
     *
     * Filters RTT samples and determines minRtt and baseRtt for adaptive behavior.
     *
     * \param tcb internal congestion state
     * \param segmentsAcked count of segments ACKed
     * \param rtt last RTT
     */
    void PktsAcked(Ptr<TcpSocketState> tcb, uint32_t segmentsAcked, const Time& rtt) override;

    /**
     * \brief Enable/disable Vegas algorithm depending on the congestion state
     *
     * \param tcb internal congestion state
     * \param newState new congestion state to which the TCP is going to switch
     */
    void CongestionStateSet(Ptr<TcpSocketState> tcb,
                            const TcpSocketState::TcpCongState_t newState) override;

    /**
     * \brief Adjust cwnd following Vegas-A adaptive algorithm
     *
     * Adaptive algorithm adjusts cwnd dynamically based on thresholds (a and b),
     * which are modified depending on network conditions.
     *
     * \param tcb internal congestion state
     * \param segmentsAcked count of segments ACKed
     */
    void IncreaseWindow(Ptr<TcpSocketState> tcb, uint32_t segmentsAcked) override;

    /**
     * \brief Get slow start threshold following Vegas principle
     *
     * \param tcb internal congestion state
     * \param bytesInFlight bytes in flight
     *
     * \return the slow start threshold value
     */
    uint32_t GetSsThresh(Ptr<const TcpSocketState> tcb, uint32_t bytesInFlight) override;

    Ptr<TcpCongestionOps> Fork() override;

  private:
    /**
     * \brief Enable Vegas algorithm to start taking Vegas samples
     *
     * Vegas algorithm is enabled in the following situations:
     * 1. at the establishment of a connection
     * 2. after an RTO
     * 3. after fast recovery
     * 4. when an idle connection is restarted
     *
     * \param tcb internal congestion state
     */
    void EnableVegas(Ptr<TcpSocketState> tcb);

    /**
     * \brief Stop taking Vegas samples
     */
    void DisableVegas();

  private:
    uint32_t m_alpha;             //!< Static Alpha threshold, lower bound of packets in network
    uint32_t m_beta;              //!< Static Beta threshold, upper bound of packets in network
    uint32_t m_gamma;             //!< Gamma threshold, limit on increase
    Time m_baseRtt;               //!< Minimum of all Vegas RTT measurements seen during connection
    Time m_minRtt;                //!< Minimum of all RTT measurements within last RTT
    uint32_t m_cntRtt;            //!< Number of RTT measurements during last RTT
    bool m_doingVegasNow;         //!< If true, do Vegas for this RTT
    SequenceNumber32 m_begSndNxt; //!< Right edge during last RTT

    double m_prevThroughput;
};

} // namespace ns3

#endif // TcpVegasModified_H
