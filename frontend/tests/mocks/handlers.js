import { http, HttpResponse } from 'msw';

export const handlers = [
  // Disasters
  http.get('/api/v1/disasters', () => {
    return HttpResponse.json({
      content: [
        {
          disasterId: '550e8400-e29b-41d4-a716-446655440000',
          name: 'Pacific Coast Flood Relief 2026',
          type: 'FLOOD',
          severityLevel: 'CATASTROPHIC',
          status: 'ACTIVE',
        },
      ],
      totalElements: 1,
    });
  }),

  // Donors & Donations
  http.get('/api/v1/donors/:donorId/donations', () => {
    return HttpResponse.json([
      {
        donationId: 'don-1001-mock',
        type: 'MONETARY',
        amount: 500,
        currency: 'USD',
        status: 'RECEIVED',
        createdAt: '2026-10-29T12:00:00Z',
      },
    ]);
  }),

  http.post('/api/v1/donors/:donorId/donations', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json(
      {
        donationId: 'don-new-123',
        status: 'REGISTERED',
        createdAt: new Date().toISOString(),
        ...body,
      },
      { status: 201 }
    );
  }),

  http.get('/api/v1/donors/:donorId/impact', () => {
    return HttpResponse.json({
      totalMonetary: 15400,
      totalInKindKg: 620,
      activeMissionsCount: 3,
      livesReached: 2150,
    });
  }),

  // Receivers & Aid Requests
  http.get('/api/v1/receivers/:receiverId/requests', () => {
    return HttpResponse.json([
      {
        requestId: 'req-2001-mock',
        itemName: 'Clean Drinking Water Rations',
        quantity: 200,
        unit: 'cases',
        urgency: 'HIGH',
        status: 'IN_TRANSIT',
        createdAt: '2026-10-29T10:00:00Z',
      },
    ]);
  }),

  http.post('/api/v1/receivers/:receiverId/requests', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json(
      {
        requestId: 'req-new-456',
        status: 'SUBMITTED',
        createdAt: new Date().toISOString(),
        ...body,
      },
      { status: 201 }
    );
  }),

  http.patch('/api/v1/receivers/:receiverId/requests/:requestId/confirm-delivery', () => {
    return HttpResponse.json({ status: 'FULFILLED' });
  }),

  // Transport & Shipments
  http.get('/api/v1/transport/shipments', () => {
    return HttpResponse.json([
      {
        shipmentId: 'sh-3001-mock',
        status: 'ASSIGNED',
        pickupAddress: { street: 'Main Depot Sector 1' },
        deliveryAddress: { street: 'Camp Hope Alpha' },
        scheduledPickupAt: '2026-10-29T14:00:00Z',
      },
    ]);
  }),

  http.patch('/api/v1/transport/shipments/:shipmentId/pickup', () => {
    return HttpResponse.json({ status: 'PICKED_UP' });
  }),

  http.patch('/api/v1/transport/shipments/:shipmentId/deliver', () => {
    return HttpResponse.json({ status: 'DELIVERED' });
  }),

  http.patch('/api/v1/transport/shipments/:shipmentId/dispute', () => {
    return HttpResponse.json({ status: 'DISPUTED' });
  }),

  http.post('/api/v1/transport/road-closures', () => {
    return HttpResponse.json({ success: true }, { status: 201 });
  }),

  // Matching & Allocations
  http.get('/api/v1/matching/allocations', () => {
    return HttpResponse.json([
      {
        allocationId: 'alloc-4001-mock',
        itemName: 'Emergency Medical Trauma Kits',
        allocatedQuantity: 50,
        warehouseName: 'Warehouse Delta',
        urgency: 'CRITICAL',
        status: 'SUGGESTED',
      },
    ]);
  }),

  http.patch('/api/v1/matching/allocations/:id/approve', () => {
    return HttpResponse.json({ status: 'APPROVED' });
  }),

  http.patch('/api/v1/matching/allocations/:id/reject', () => {
    return HttpResponse.json({ status: 'REJECTED' });
  }),

  // Verification Cases
  http.get('/api/v1/verification/cases', () => {
    return HttpResponse.json([
      {
        caseId: 'case-5001-mock',
        receiverName: 'Camp Alpha Relief Committee',
        receiverId: 'rec-1234-uuid',
        status: 'OPEN',
        openedAt: '2026-10-29T09:00:00Z',
      },
    ]);
  }),

  http.patch('/api/v1/verification/cases/:id/approve', () => {
    return HttpResponse.json({ status: 'APPROVED' });
  }),

  http.patch('/api/v1/verification/cases/:id/reject', () => {
    return HttpResponse.json({ status: 'REJECTED' });
  }),

  // Analytics Dashboard
  http.get('/api/v1/analytics/dashboard', () => {
    return HttpResponse.json({
      donationSummary: {
        totalMonetaryAmount: 450000,
        totalInKindKg: 25000,
      },
      requestSummary: {
        totalRequests: 140,
      },
      shipmentSummary: {
        totalShipments: 85,
        deliveredCount: 65,
      },
      activeDisasters: 2,
    });
  }),
];
